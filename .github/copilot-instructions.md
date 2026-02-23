# Copilot Instructions for dp-saksbehandling

Backend application for handling case processing (saksbehandling) related to unemployment benefits (dagpenger) at NAV.

## Build, Test, and Lint

```bash
# Build entire project
./gradlew build

# Run all tests
./gradlew test

# Run tests for specific module
./gradlew :modell:test
./gradlew :mediator:test

# Run a single test class
./gradlew test --tests OppgaveTilstandTest

# Run a single test method (use backslash for spaces)
./gradlew test --tests "OppgaveTilstandTest.Skal på nytt kunne tildele*"

# Lint (ktlint runs automatically on compile, but can be run manually)
./gradlew ktlintCheck
./gradlew ktlintFormat
```

## Architecture

### Module Structure

- **modell**: Pure domain model with no external dependencies. Contains domain entities, state machines, and events.
- **mediator**: Application layer with API, database, and messaging. Orchestrates domain logic via mediator classes.
- **openapi**: API contract definitions. Models are generated from `saksbehandling-api.yaml` using Fabrikt.
- **streams-consumer**: Kafka Streams consumers for external data.

### Key Domain Concepts

The domain follows Norwegian terminology:
- **Sak** (Case): Aggregate root containing multiple behandlinger
- **Behandling** (Treatment/Processing): A specific case processing instance
- **Oppgave** (Task): Work item with complex state machine for saksbehandlere
- **Saksbehandler** (Case handler): User processing tasks
- **KlageBehandling**: Appeal/complaint handling workflow
- **Innsending**: Document submission handling
- **Utsending**: Outbound document distribution

### Oppgave State Machine

```
Opprettet → KlarTilBehandling → UnderBehandling → FerdigBehandlet
                  ↑     ↑            ↓    ↓
                  │     └── PåVent ←─┘    └→ KlarTilKontroll → UnderKontroll
                  │                                  ↑              ↓
                  └──────────────────────────────────┼── returner ──┘
                                                     └── fjernAnsvar
```

Terminal states: `FerdigBehandlet`, `Avbrutt`, `AvbruttMaskinelt`. Most states can transition to `Avbrutt` via `BehandlingAvbrutt`.

### Event-Driven Architecture

Uses NAV's **Rapids and Rivers** pattern for Kafka messaging:
- **Mottak classes** (e.g., `BehandlingOpprettetMottak`) implement `River.PacketListener` to consume events
- Events are filtered by `@event_name` in JSON messages
- Mediators publish responses via `rapidsConnection.publish()`

#### Inbound Events (Consumed)

| Event Name | Mottak Class | Purpose |
|------------|--------------|---------|
| `behandling_opprettet` | `BehandlingOpprettetMottak`, `SøknadBehandlingOpprettetMottak` | New case processing created |
| `forslag_til_behandlingsresultat` | `ForslagTilBehandlingsresultatMottak` | Proposed result ready for review |
| `behandlingsresultat` | `BehandlingsresultatMottak` | Final processing result |
| `behandling_avbrutt` | `BehandlingAvbruttMottak` | Processing was cancelled |
| `arenasink_vedtak_opprettet` | `ArenaSinkVedtakOpprettetMottak` | Legacy Arena system verdict |
| `KlageAnkeVedtak` | `KlageinstansVedtakMottak` | Appeal decision from Klageinstans |
| `klage_behandling_utført` | `KlageBehandlingUtførtMottak` | Appeal processing completed |
| `utsending_distribuert` | `UtsendingDistribuertMottakForKlage` | Document successfully distributed |
| `behov` | Various behovløsere | Request/response pattern for needs |

#### Outbound Events (Published)

| Event Name | Publisher | Purpose |
|------------|-----------|---------|
| `avbryt_behandling` | `SakMediator` | Request to cancel a processing |
| `vedtak_fattet_utenfor_arena` | `BehandlingsresultatMottakForSak` | Verdict made outside Arena |
| `klage_behandling_utført` | `KlageMediator` | Appeal processing completed |
| `oppgave_til_statistikk_v3` | `StatistikkJob` | Task statistics for reporting |
| `saksbehandling_alert` | `AlertManager` | System alerts and alarms |

#### Behov (Need/Solution Pattern)

The `behov` event type implements a request/response pattern:
- Mediators publish a `behov` message with `@behov` specifying what's needed
- Behovløsere (need solvers) listen for specific `@behov` types and add solutions
- Common behov: `MeldingOmVedtakProdusent`, `Innsending`, `OversendKlageinstans`

### Mediator Pattern

Six primary mediators orchestrate domain operations:
- `SakMediator` - Case lifecycle
- `OppgaveMediator` - Task workflow
- `UtsendingMediator` - Document distribution
- `InnsendingMediator` - Submission processing
- `KlageMediator` - Appeal handling
- `PersonMediator` - Person data sync

## Conventions

### Testing

- **JUnit 5** as test runner with **Kotest** assertions (`shouldBe`, `shouldThrow`, etc.)
- **MockK** for mocking
- Use `TestRapid` for testing Kafka message flows
- Test helpers in `testFixtures` source set (e.g., `ModellTestHelper.lagOppgave()`)
- Database tests use `DBTestHelper.withMigratedDb { ds -> ... }`

### Code Style

- ktlint enforced automatically on compilation
- All domain entities use Norwegian naming
- State transitions logged via `Tilstandslogg`
- Sealed class hierarchies for events (`Hendelse`) and domain results

### OpenAPI Code Generation

Models are generated from `openapi/src/main/resources/saksbehandling-api.yaml`:
- Generated classes have `DTO` suffix
- Run `./gradlew :openapi:fabriktGenerate` to regenerate
- Generated code is in `openapi/build/generated/`

### Database

- PostgreSQL with Flyway migrations (in `mediator/src/main/resources/db/migration/`)
- Repository interfaces in domain, implementations in mediator
- Pattern: `PostgresXxxRepository` implements `XxxRepository`

### Kotlin & Build

- Kotlin JVM toolchain **21**
- All modules apply `common` convention plugin from buildSrc (configures Kotlin, ktlint, JUnit Platform)
- `streams-consumer` uses Avro plugin for Kafka schema code generation
