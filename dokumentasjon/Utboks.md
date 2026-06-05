# Utboks (transactional outbox)

Utboksen sikrer at meldinger til Rapids & Rivers publiseres atomisk sammen med
domeneendringen som utløser dem. Den løser **dual-write-problemet**: tidligere
skrev mediatorene til databasen og publiserte til Kafka i to uavhengige steg, så
en krasj mellom dem kunne gi en lagret tilstand uten tilhørende melding (eller
omvendt).

Med utboksen skrives meldingen til en `kafka_utboks_v1`-rad i **samme
databasetransaksjon** som domeneendringen. En bakgrunnsjobb plukker opp ventende
rader og publiserer dem til rapid etterpå.

## Kontrakt

Mediatorene bruker kun sender-seamen:

```kotlin
interface Utboks {
    fun send(
        key: String,                      // partisjoneringsnøkkel, typisk fnr
        message: String,                  // ferdig JSON (JsonMessage.toJson())
        ctx: Transaksjonskontekst.Aktiv,  // tvinger kaller inn i en transaksjon
    )
}
```

Det viktige er `ctx: Transaksjonskontekst.Aktiv`. Typen er **kompilator-håndhevet
holdbarhet**: du kan ikke kalle `send` uten en aktiv transaksjon, så det er umulig
å enqueue en melding utenfor en transaksjon ved et uhell.

### Slik brukes den i en mediator

Injiser `Utboks` og `Transaksjoner`, og gjør lagring + send i samme blokk:

```kotlin
transaksjoner.transaksjon { ctx ->
    oppgaveRepository.lagre(oppgave, ctx)
    utboks.send(
        key = oppgave.personIdent(),
        message = JsonMessage.newMessage(eventName = "...", map = ...).toJson(),
        ctx = ctx,
    )
}
```

`Transaksjoner.transaksjon { ctx -> ... }` (i `db/Transaksjonskontekst.kt`) starter
transaksjonen og gir deg en `Aktiv`-kontekst. Det finnes også en overload
`transaksjon(ctx) { ... }` som gjenbruker en eksisterende `Aktiv` eller starter en
ny hvis `ctx` er `IkkeAktiv` — nyttig når en metode kan kalles både frittstående og
inni en pågående transaksjon.

## Leveringssemantikk: at-least-once

Publiseringen gjør to separate operasjoner: `publish` til rapid (steg 1) og
markering av raden som `SENDT` (steg 2). Krasjer poden mellom dem, blir meldingen
re-publisert ved neste poll.

**Utboksen eliminerer altså _tapte_ meldinger, ikke _dupliserte_.** Alle
konsumenter må derfor være idempotente. Se «Oppfølging» nederst.

## Komponenter

| Klasse | Ansvar |
| --- | --- |
| `Utboks` | Sender-seam for mediatorene (`send`). |
| `UtboksVedlikehold` | Jobb-seam (`publiserVentende`, `slettGamleSendte`). |
| `PostgresRapidUtboks` | All logikk: bestemmer `UtboksTilstand`, publiserer, rydder. Implementerer begge seam. |
| `UtboksRepository` / `PostgresUtboksRepository` | Persistens-seam. Uvitende om tilstandsverdier — tar `tilstand` som `String`. |
| `UtboksTilstand` | `PENDING` (ikke publisert) → `SENDT` (publisert, kan ryddes). |
| `UtboksPubliseringJob` | Kjører `publiserVentende()` hvert **5. sekund**. |
| `UtboksOppryddingJob` | Kjører `slettGamleSendte()` **daglig 03:30**; sletter `SENDT` eldre enn `utboksLevetidSendte` (default 7 dager). |

### Publiseringsgarantier

- **Global FIFO** — `ORDER BY id` i repositoryet.
- **Stopper ved første feil** og retryer ved neste poll. Bevisst: `key=fnr` går til
  én rapid-strøm, så feil er normalt globale og bør feile høyt heller enn å hoppe
  over enkeltmeldinger.
- **GDPR** — `key` (fnr) og meldingsinnhold logges kun til `tjenestekall`
  (sikkerlogg), aldri til vanlig logg.

## Database

Tabell `kafka_utboks_v1` (omdøpt fra `outbox` i `V120__UTBOKS_RENAME.sql`; opprettet
i `V119__OUTBOX.sql`):

```
id                   BIGSERIAL PRIMARY KEY
key                  TEXT  -- partisjoneringsnøkkel (fnr)
message              TEXT  -- ferdig JSON
status               TEXT  -- PENDING | SENDT
registrert_tidspunkt TIMESTAMP  -- Europe/Oslo
endret_tidspunkt     TIMESTAMP  -- Europe/Oslo, oppdateres av trigger
```

Partielt indeks `idx_kafka_utboks_v1_pending ON (id) WHERE status = 'PENDING'` gir
billig FIFO-uthenting av ventende rader.

## Observability

Prometheus-metrikker (registrert i `PostgresRapidUtboks`):

- `dp_saksbehandling_utboks_nye_meldinger_total` — counter, antall enqueuet.
- `dp_saksbehandling_utboks_publiserte_meldinger_total{status}` — counter,
  `success`/`failed`.
- `dp_saksbehandling_utboks_ventende_meldinger` — gauge, antall `PENDING`. Stigende
  gauge = publiseringen henger (poll feiler eller står stille).

Grafana-dashboard for jobbene finnes (se sesjonsartefakt
`utboks-grafana-dashboard.json`). Merk NAIS-quirken: `job`-label overskrives til
`nais-system/...`; ekte jobbnavn ligger i `exported_job`.

## Migreringsstatus per 2026-06-05

Mediatorer migrert fra direkte `rapidsConnection.publish` til `utboks.send`:

- ✅ **UtsendingMediator**
- ✅ **KlageMediator** — `opprettKlage()` (klage_behandling_opprettet) og
  `vedtakDistribuert()` (OversendKlageinstans-behov).
- ✅ **OppgaveMediator** — `avbryt()` (avbryt_behandling),
  `sendSøknadsavklaringBehov()` (behov) og `BEHANDLING_IKKE_FUNNET`-alert. Etter
  dette har OppgaveMediator ingen `RapidsConnection`-avhengighet.

### Migreringen er ferdig

Alle mediator-dual-writes er migrert. `SakMediator.sendAvbrytBehandling()` er
**ikke** en utboks-kandidat: den kalles kun i `.onFailure`-grenen av `opprettSak()`
(person er skjermet/adressebeskyttet), der ingen `sakRepository.lagre` skjer. Det
finnes dermed ingen dual-write — publiseringen er en enkeltoperasjon uten
forutgående DB-write å beskytte. `SakMediator` beholder `RapidsConnection` for
`sendAlertTilRapid` (fire-and-forget-varsel).

`context.publish(...)` i Mottak/Behovløsere (f.eks. `MeldingOmVedtakProdusentBehovløser`,
`InnsendingBehovløser`, `BehandlingsresultatMottakForSak`) er **rapid-svar innen
River-konteksten**, ikke dual-writes — disse skal ikke til utboksen.
`AlertManager.RapidsConnection.sendAlertTilRapid` (brukt av AlarmJobs +
SakMediator) er fire-and-forget-varsler, heller ikke utboks-kandidater.

## Oppfølging (krever idempotente konsumenter)

Siden leveringen er at-least-once, må disse konsumentene tåle duplikater:

- **Kabal** — `OversendKlageinstans` (fra KlageMediator).
- **dp-behandling** — `avbryt_behandling` (fra OppgaveMediator).
- **dp-behov-distribuering** — distribusjonsbehov.

For utsending er konsument-idempotens designet via domenets tilstandsmaskin (no-op
ved reprise i stedet for å kaste). Se sesjonsartefakt
`utsending-idempotens-design.md`.

### Utsatt

- **Jobb-alarmer** for utboks-jobbene: `UtboksDrenererIkke` (stillstand i
  publisering) er på plass i `.nais/alerts.yaml` (dev+prod). Mer finkornede
  alarmer (per-jobb feilrate o.l.) utsatt til det er mer trafikk i prod.
- **Alert-semantikk**: `BEHANDLING_IKKE_FUNNET` går nå at-least-once/async via
  utboks i en frittstående transaksjon (kun `utboks.send`, ingen domeneendring å
  rulle tilbake), i stedet for synkron fire-and-forget. Bevisst valg for å fjerne
  siste `RapidsConnection`-avhengighet fra OppgaveMediator.

## Poison-meldinger og head-of-line blocking (akseptert restrisiko)

`publiserVentende` publiserer strengt FIFO (`ORDER BY id`) og **stopper ved første
feil** (`break`) — både ved synkront kast og ved `FailedMessage` fra rapiden. Det er
riktig for *transiente/globale* feil (broker nede, timeout): hele køen retryes ved
neste poll, og ingenting tapes (raden forblir PENDING til bekreftet leveranse).

Svakheten: en *permanent, deterministisk* feil på én melding ville stoppe hele køen
i det uendelige (head-of-line blocking), siden samme rad hentes og feiler hver poll.

**Hvorfor dette er akseptert som restrisiko, ikke fikset:**

- Den eneste realistiske permanente per-melding-feilen var `RecordTooLargeException`
  fra `ArkiverbartBrevBehov` (base64-HTML inline). Brevene er i praksis **langt under**
  Kafka-grensen (default 1 MB), så vektoren finnes ikke.
- Øvrige meldinger er små JSON-strenger som allerede er serialisert (`JsonMessage.toJson()`)
  *før* utboks — serialiseringsfeil ved send er ~umulig.
- Gjenstående feilklasser (broker nede, ACL, ukjent topic) er **transiente/globale**,
  ikke per-melding — der er `break` + retry korrekt respons.

⇒ Klassen «permanent per-melding-feil» er tilnærmet tom. `break`-på-feil er dermed
riktig design, ikke en svakhet.

**Sikkerhetsnett:** `UtboksDrenererIkke`-alarmen (`.nais/alerts.yaml`) fyrer hvis
`dp_saksbehandling_utboks_ventende_meldinger` holder seg `> 0` i 10 min. Skulle en
ukjent permanent feil likevel oppstå, oppdages den reaktivt — og ingenting tapes
imens (alt forblir PENDING).

**Hvis det noen gang skjer:** vurder per-key (per-fnr) `FEILET`-skip — flagg den
giftige raden `FEILET` og ekskludér kun *den personens* køede meldinger (`NOT EXISTS`
på `key`), så øvrige fnr drenerer. Det bevarer per-fnr-rekkefølge (den eneste orden
Kafka faktisk gir nedstrøms, siden `key = ident` partisjonerer per person) og isolerer
blast radius til én person. Bygges først når en faktisk feilsignatur finnes — ikke
spekulativt.
