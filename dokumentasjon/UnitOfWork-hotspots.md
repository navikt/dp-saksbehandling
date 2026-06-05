# UnitOfWork — Hotspots for cross-repository transaksjoner

## Bakgrunn

dp-saksbehandling har flere mediator-flows der multiple repositories skrives sekvensielt uten delt
transaksjon. Hver `repository.lagre(...)` er sin egen transaksjon. Ved feil mellom stegene kan vi
ende opp med inkonsistent tilstand i databasen.

Mønsteret fra dp-behandling (`PostgresUnitOfWork` + `DatabaseSession`) løser dette ved å sende én
felles `Session` gjennom alle repo-kall innenfor en transaksjon.

### Designprinsipp

- **Kun lokale DB-writes** i transaksjonen
- **Ekstern I/O** (HTTP-kall, Kafka publish) **utenfor** transaksjonsgrensen
- For DB↔event-konsistens: vurder outbox-pattern

---

## Hotspots

### 🔴 Høy risiko — rene DB-writes som kan samles i én transaksjon

| # | Flow | Fil | Repo-kall (sekvensielle) |
|---|------|-----|--------------------------|
| 1 | `OppfølgingMediator.taImot()` | `oppfolging/OppfølgingMediator.kt:31` | `sakMediator.lagreBehandling` → `oppfølgingRepo.lagre` → `oppgaveRepo.lagre` |
| 2 | `InnsendingMediator.taImotEttersendingTilSøknad()` | `innsending/InnsendingMediator.kt:61` | `innsendingRepo.lagre` → `sakMediator.knytt...` → `oppgaveRepo.lagre` |
| 3 | `InnsendingMediator.taImotInnsendingPåSisteSak()` | `innsending/InnsendingMediator.kt:81` | `innsendingRepo.lagre` → `sakMediator.knytt...` → `oppgaveRepo.lagre` |
| 4 | `KlageMediator.opprettManuellKlage()` | `KlageMediator.kt:109` | `klageRepo.lagre` → `sakMediator.knyttTilSak` → `oppgaveRepo.lagre` → `oppgaveRepo.lagre` (tildel) |
| 5 | `KlageMediator.avbrytKlage()` | `KlageMediator.kt:244` | `klageRepo.lagre` → `oppgaveRepo.lagre` (ferdigstill) |

### 🟡 Medium risiko — blander DB-writes med ekstern I/O

| # | Flow | Fil | Repo-kall | Ekstern I/O |
|---|------|-----|-----------|-------------|
| 6 | `KlageMediator.opprettKlage()` | `KlageMediator.kt:54` | `klageRepo.lagre` → `sakMediator.knyttTilSak` → Kafka → `oppgaveRepo.lagre` | Kafka publish mellom steg 2 og 3 |
| 7 | `OppgaveMediator.ferdigstillOppgaveMedUtsending()` | `OppgaveMediator.kt:598` | `hentEllerOpprettUtsending` → HTTP → `oppgaveRepo.lagre` | HTTP (godkjenn/beslutt). Selvhelende via `behandlingsresultat`-event — se notat |
| 8 | `KlageMediator.behandlingUtført()` | `KlageMediator.kt:178` | HTTP → `utsendingRepo.lagre` → `klageRepo.lagre` → Kafka | HTTP (brev) + Kafka publish |
| 9 | `OppfølgingMediator.ferdigstill()` | `oppfolging/OppfølgingMediator.kt:85` | `oppfølgingRepo.lagre` → [HTTP aksjon] → `oppgaveRepo.lagre` → `oppfølgingRepo.lagre` | HTTP (opprett klage/behandling) — bevisst design, mitigert av OppfølgingAlarmJob |
| 10 | `InnsendingMediator.ferdigstill()` | `innsending/InnsendingMediator.kt:113` | `innsendingRepo.lagre` → [aksjon] → `oppgaveRepo.lagre` → `innsendingRepo.lagre` | Aksjonen kan inneholde HTTP |

### 🟢 Lav risiko — samme repository eller allerede mitigert

| # | Flow | Fil | Beskrivelse |
|---|------|-----|-------------|
| 11 | `OppgaveMediator.håndter(VedtakFattet)` | `OppgaveMediator.kt:691` | 2x `oppgaveRepo.lagre` — samme repo, idempotent |
| 12 | `OppgaveMediator.opprettEllerOppdaterOppgave()` | `OppgaveMediator.kt:197` | Opptil 2x `oppgaveRepo.lagre` — samme repo |

---

## Anbefalt rekkefølge

1. ✅ **Innfør infrastruktur**: `DatabaseSession` + `PostgresUnitOfWork` (etter dp-behandling-mønster)
2. ✅ **Migrer 🔴 hotspots**: Rene DB-writes, ingen ekstern I/O — trygt å samle i transaksjon
3. **Vurder 🟡 hotspots**: For flows med HTTP/Kafka — vurder om DB-delen kan isoleres i transaksjon med I/O utenfor
4. **Outbox-pattern**: For flows der DB+event-konsistens er kritisk

## Status (2026-06-01)

Alle 🔴 røde hotspots (#1–5) er wrappet i transaksjon via PR-stack #386→#387→#389.
Alle 🟡 gule er enten wrappet (der mulig), bevisst ikke wrappet (HTTP i midten), eller mitigert med alarm-jobb.

### Nye funn fra fullstendig mediator-gjennomgang

| # | Flow | Type | Risiko | Beskrivelse |
|---|------|------|--------|-------------|
| 13 | `OppgaveMediator.endreMeldingOmVedtakKilde()` | DB+DB | Lav | `lagre(oppgave)` → `slettUtsending`. Reverser rekkefølge eller wrap i tx |
| 14 | `OppgaveMediator.avbryt()` | DB+Kafka | **Medium** | `lagre(oppgave)` → Kafka `avbryt_behandling`. Ingen alarm-jobb dekker dette. dp-behandling vet ikke om avbruddet ved krasj |
| 15 | `SakMediator.opprettSak()` | DB+DB | Lav | `finnEllerOpprettPerson` → `lagre(sakHistorikk)`. Idempotent ved retry |

### Re-vurdering av nye funn (2026-06-01)

Etter kritisk gjennomgang: **#13/#14/#15 er ikke reelle problemer** for cross-repo transaksjonshåndtering.

| # | Flow | Type | Konklusjon |
|---|------|------|------------|
| 13 | `OppgaveMediator.endreMeldingOmVedtakKilde()` | DB+DB | **Ikke problem.** API-trigget (synkron). `slettUtsending` er separat, trygg. Feiler den, får saksbehandler HTTP-feil og prøver på nytt; `endreMeldingOmVedtakKilde` er idempotent. Selvhelende ved retry |
| 14 | `OppgaveMediator.avbryt()` | DB+Kafka | **Ikke cross-repo-hotspot.** Dette er dual-write (DB+Kafka), annen problemklasse enn UoW. API-trigget. `rapidsConnection.publish` kaster sjelden; vinduet er app-krasj mellom to linjer. Akseptert R&R-tradeoff på tvers av hele kodebasen |
| 15 | `SakMediator.opprettSak()` | DB+DB | **Ikke problem.** Kafka-trigget → River reprosesserer ved feil (onPacket kaster = ingen ack). `finnEllerOpprettPerson` idempotent. Selvhelende |

### Funn som ble fikset

| # | Flow | Fil | Beskrivelse | Status |
|---|------|-----|-------------|--------|
| 16 | `InnsendingMediator.automatiskFerdigstill()` | `innsending/InnsendingMediator.kt:133` | `innsendingRepo.lagre` → `oppgaveMediator.avbrytOppgave` (cross-repo, ingen HTTP imellom). Var ikke wrappet — strukturelt identisk med #2/#3 | ✅ Wrappet i tx (la til `ctx`-param på `avbrytOppgave`) + rollback-test |

**Konklusjon:** Etter wrapping av #16 er alle ekte cross-repo DB-write-flows uten ekstern I/O dekket. Gjenstående uvwrappede flows har enten HTTP/Kafka imellom (bevisst, mitigert av alarm-jobber) eller er selvhelende via River-retry/idempotens.

## Notater

- `OppfølgingMediator.ferdigstill()` har bevisst split-transaksjon (se Oppfølging.md) — mitigert av OppfølgingAlarmJob
- `InnsendingMediator.ferdigstill()` har bevisst split-transaksjon — mitigert av InnsendingAlarmJob
- `ferdigstillOppgaveMedUtsending()` er **selvhelende via Kafka**, ikke via kompenserende delete. Rekkefølgen er bevisst: `hentEllerOpprettUtsending` oppretter utsendingen *før* HTTP-kallet (`godkjenn/beslutt`). Hvis HTTP timer ut / feiler, men dp-behandling faktisk fullfører behandlingen, kommer et `behandlingsresultat`-event som (a) `BehandlingsresultatMottak` ferdigstiller oppgaven, og (b) `BehandlingsresultatMottakForUtsending` → `startUtsendingForVedtakFattet` starter den allerede opprettede utsendingen. Den tidlige opprettelsen er **load-bearing**: `startUtsendingForVedtakFattet` bruker `finnUtsendingForBehandlingId(...)?.let { ... }` og starter kun en utsending som allerede finnes. Begge stegene er idempotente (`oppgave.ferdigstill` → `Handling.INGEN` ved allerede ferdig; utsendingens tilstandsmaskin no-op'er ved reprise), så saksbehandlers retry og Kafka-recovery kan skje samtidig uten dobbeltkjøring. Feiler HTTP fordi dp-behandling rullet tilbake, kommer ingen event — men da er ingenting fullført, og saksbehandlers retry er riktig vei.
- `OppgaveTilstandAlertJob` sjekker kun oppgaver stuck i `OPPRETTET` — dekker ikke #14
