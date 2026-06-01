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
| 7 | `OppgaveMediator.ferdigstillOppgaveMedUtsending()` | `OppgaveMediator.kt:581` | `utsendingRepo.lagre` → HTTP → `oppgaveRepo.lagre` | HTTP (godkjenn/beslutt). Har manuell kompensering (slett utsending ved feil) |
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

**Anbefaling for #14:** Lag `AvbrytBehandlingAlarmJob` som finner oppgaver i tilstand `Avbrutt` der behandlingen fortsatt er aktiv i dp-behandling, eller bruk outbox-pattern for `avbryt_behandling`-eventen.

## Notater

- `OppfølgingMediator.ferdigstill()` har bevisst split-transaksjon (se Oppfølging.md) — mitigert av OppfølgingAlarmJob
- `InnsendingMediator.ferdigstill()` har bevisst split-transaksjon — mitigert av InnsendingAlarmJob
- `ferdigstillOppgaveMedUtsending()` har allerede compensating delete — fungerer, men er sårbar for app-krasj mellom steg 1 og 3
- `OppgaveTilstandAlertJob` sjekker kun oppgaver stuck i `OPPRETTET` — dekker ikke #14
