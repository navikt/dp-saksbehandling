# Oppfølging

Oppfølging er en fleksibel oppgavetype som lar saksbehandlere håndtere arbeid som ikke passer inn i eksisterende behandlingsflyter (søknad, meldekort, klage etc.). Oppgaver kan opprettes automatisk via Kafka-hendelser fra andre systemer, eller manuelt av saksbehandler via REST API.

## Typiske brukstilfeller

- PDL-hendelser (adresseendring, dødsfall) som krever manuell oppfølging
- Meldekort som trenger korrigering
- Henvendelser fra NKS/veiledere
- Bro fra Gosys
- Generelt arbeid som ikke har en tilhørende søknad eller vedtak

## Arkitektur

### Datamodell

```
Person ← Oppfølging ──→ Behandling (UtløstAvType.OPPFØLGING)
                         └──→ Oppgave (emneknagger = { aarsak })
```

`Oppfølging` er et selvstendig domeneobjekt som oppretter et `Behandling + Oppgave`-par. Dette er et bevisst valg for å gjenbruke eksisterende infrastruktur (tilstandsmaskiner, tilgangskontroll, saksbehandler-tildeling) uten å refaktorere `Oppgave` til å eksistere uten `Behandling`.

### Tilstandsmaskin

```
BEHANDLES ──→ FERDIGSTILL_STARTET ──→ FERDIGSTILT
```

Enklere enn `Oppgave`s tilstands maskin. `FERDIGSTILL_STARTET` er en transient tilstand som settes før ferdigstillingsflytens eksterne kall (opprette klage, ny behandling, osv.). `FERDIGSTILT` settes etter at alle steg er fullført.

#### Distribuerte transaksjoner og AlarmJob

`ferdigstill()`-flyten består av flere separate DB-operasjoner med et eksternt HTTP-kall i midten — dette kan ikke atomiseres i én DB-transaksjon. Risikoen mitigeres av `OppfølgingAlarmJob` som kjører daglig og varsler via `saksbehandling_alert` event dersom en Oppfølging sitter fast i `FERDIGSTILL_STARTET` i mer enn 24 timer. Dette er et **bevisst design-valg**.

### Inngangsporter

Samme `OpprettOppfølgingHendelse` brukes fra begge kildetyper:

```
Kafka (opprett_oppgave) ──→ OpprettOppgaveMottak ──┐
                                                    ├──→ OppfølgingMediator.taImot()
REST POST /oppfolging  ─────────────────────┘
```

### Kafka-format

```json
{
  "@event_name": "opprett_oppgave",
  "ident": "12345678901",
  "tittel": "Sjekk adresseendring",
  "beskrivelse": "PDL registrerte ny adresse",
  "emneknagg": "AdresseEndring",
  "frist": "2026-05-01",
  "strukturertData": {
    "pdlHendelseId": "abc-123"
  }
}
```

> **Merk:** Kafka-feltet heter `emneknagg` (bakoverkompatibilitet), men mappes internt til `aarsak`.

### REST API

```
POST   /oppfolging              Opprett ny oppfølging
GET    /oppfolging/{behandlingId}  Hent oppfølging
PUT    /oppfolging/{behandlingId}/ferdigstill  Ferdigstill
```

## Ferdigstill-aksjonene

Saksbehandler velger utfall ved ferdigstilling:

| Aksjon | Beskrivelse |
|--------|-------------|
| `AVSLUTT` | Ferdigstill uten oppfølging |
| `OPPRETT_KLAGE` | Opprett klage på eksisterende sak |
| `OPPRETT_MANUELL_BEHANDLING` | Opprett ny manuell dagpengebehandling |
| `OPPRETT_REVURDERING_BEHANDLING` | Opprett revurdering |
| `OPPRETT_OPPFOLGING` | Opprett ny oppfølging som oppfølging av denne |

### beholdOppgaven

Både ved opprettelse og ved `OPPRETT_OPPFOLGING`-aksjon kan saksbehandler velge å få den nye oppgaven tildelt seg selv (`beholdOppgaven: true`).

## Frist-støtte

Dersom `frist` er satt, opprettes `Oppgave` i `PåVent`-tilstand med `utsattTil = frist`. Den eksisterende `OppgaveFristUtgåttJob` reaktiverer oppgaven automatisk når fristen passerer — ingen ny infrastruktur nødvendig.

## strukturertData

Fri JSON-blob for domene-spesifikk kontekst. Backend lagrer og eksponerer den uten å tolke innholdet. Frontend kan bruke den til å vise relevant kontekst for saksbehandler.

```json
"strukturertData": {
  "meldekortId": "MK-2026-01",
  "periodeId": "2026-W04"
}
```

## Relevant kode

| Fil | Beskrivelse |
|-----|-------------|
| `modell/.../oppfolging/Oppfølging.kt` | Domeneobjekt med tilstandsmaskin |
| `modell/.../oppfolging/OppfølgingAksjon.kt` | 5 aksjontyper |
| `modell/.../hendelser/OpprettOppfølgingHendelse.kt` | Felles hendelse for Kafka og REST |
| `mediator/.../oppfolging/OppfølgingMediator.kt` | Orkestrator |
| `mediator/.../oppfolging/OppfølgingApi.kt` | REST-endepunkter |
| `mediator/.../oppfolging/OppfølgingBehandler.kt` | Utfører aksjonene |
| `mediator/.../oppfolging/OppfølgingAlarmJob.kt` | Daglig sjekk for fast-sittende FERDIGSTILL_STARTET (>24t) |
| `mediator/.../oppfolging/OpprettOppgaveMottak.kt` | Kafka-konsument |
| `db/migration/V109__CREATE_OPPFOLGING.sql` | Databaseskjema |

## Gjenstår / on hold

- **årsak-emneknagg for `settPåVent`**: `settPåVent()` legger ikke til årsak-emneknagg. Avventer avklaring med frontend om behovet.
- **`FerdigstillOppfølgingRequest` på Innsending-endepunktet**: Utsatt til etter første versjon.
