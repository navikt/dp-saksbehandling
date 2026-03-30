# Tilbakekreving (Debt Recovery) Feature

## Overview

Tilbakekreving handles debt recovery cases for dagpenger (unemployment benefits). Events come from NAV's `familie-tilbake` system via Kafka and are processed into oppgaver (tasks) for saksbehandlere.

## Kafka Contract

- **Topic:** `tilbake.privat-tilbakekreving-dagpenger` (configured via KAFKA_EXTRA_TOPIC in nais.yaml)
- **Key:** personident (fnr/dnr), **Value:** JSON with StringSerializer
- **Filter:** `hendelsestype=behandling_endret`, `versjon=1`

### JSON Structure

Root fields (flattened from metadata): `hendelsestype`, `versjon`, `eksternFagsakId`, `eksternBehandlingId`, `hendelseOpprettet`

Nested `tilbakekreving` object: `behandlingId`, `sakOpprettet`, `varselSendt`, `behandlingsstatus`, `forrigeBehandlingsstatus`, `totaltFeilutbetaltBeløp`, `saksbehandlingURL`, `fullstendigPeriode`

### BehandlingStatus Values

`OPPRETTET`, `TIL_BEHANDLING`, `TIL_GODKJENNING`, `AVSLUTTET`

## ID Mapping (Important!)

Two different IDs are used:

- **`eksternBehandlingId`** (UUID): The dp-behandling UUID from dp-behandling. Used to **find the correct Sak** via `basertPåBehandlingErKnyttetTilSak()`.
- **`tilbakekreving.behandlingId`** (UUID): The tilbakekreving-behandling UUID from familie-tilbake. Used as the **Behandling.behandlingId** for the new tilbakekreving oppgave.

## State Machine

```
OPPRETTET → creates Oppgave in Opprettet state (just saves, no håndter)
TIL_BEHANDLING → Opprettet.håndter → KlarTilBehandling
  (saksbehandler must tildel() → UnderBehandling)
TIL_GODKJENNING → UnderBehandling.håndter → KlarTilKontroll (no beslutter) or UnderKontroll (has beslutter)
TIL_BEHANDLING (underkjent) → UnderKontroll.håndter → UnderBehandling + "Retur fra kontroll" emneknagg
AVSLUTTET → UnderKontroll.håndter → FerdigBehandlet
```

### Key Behaviors

- **KlarTilBehandling does NOT handle TilbakekrevingHendelse** — saksbehandler must tildel() first
- After underkjenning, `sisteBeslutter()` is non-null → goes directly to UnderKontroll (not KlarTilKontroll)
- Underkjenning adds emneknagger: `Retur fra kontroll`, `Tidligere kontrollert`

## File Structure (Feature-based)

### Domain Model (modell module)

- `hendelser/TilbakekrevingHendelse.kt` — Extends Hendelse sealed class. Contains nested Tilbakekreving, Periode, BehandlingStatus.
- `Oppgave.kt` — State handlers in Opprettet, UnderBehandling, UnderKontroll for TilbakekrevingHendelse.

### Application Layer (mediator module)

- `tilbakekreving/TilbakekrevingMottak.kt` — River.PacketListener, parses Kafka JSON, calls oppgaveMediator.håndter()
- `tilbakekreving/TilbakekrevingApi.kt` — GET /tilbakekreving/{behandlingId}, checks utløstAv type, gets latest hendelse from tilstandslogg
- `OppgaveMediator.kt` — håndter(TilbakekrevingHendelse): creates oppgave for OPPRETTET, delegates to oppgave.håndter() for existing

### Tests

- `tilbakekreving/TilbakekrevingMottakTest.kt` — 6 tests with mockk OppgaveMediator
- `tilbakekreving/TilbakekrevingApiTest.kt` — 4 tests (401, 404 not found, 404 wrong type, 200)
- `OppgaveTilbakekrevingTest.kt` — 10 tests for all state transitions

### API (openapi module)

- `saksbehandling-api.yaml` — Tilbakekreving path and schemas, TILBAKEKREVING in UtloestAvType enum

## Design Decisions

- `TilbakekrevingHendelse` extends `Hendelse` (sealed class in hendelser package — must stay there)
- `eksternBehandlingId` is non-nullable UUID (throws IllegalArgumentException if missing from Kafka)
- Packet parsing in private `tilbakekrevingHendelseFraPacket()` in Mottak file (modell can't depend on rapids)
- API gets tilbakekreving data from tilstandslogg (latest TilbakekrevingHendelse), not behandling.hendelse
- UtløstAvType.TILBAKEKREVING check ensures API only returns tilbakekreving oppgaver
- DTOs auto-generated from openapi spec via Fabrikt
