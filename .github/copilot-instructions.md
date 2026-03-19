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

# Regenerate OpenAPI models (runs as dependency of :openapi:compileKotlin)
./gradlew :openapi:compileKotlin
```

## Architecture

### Module Structure

- **modell**: Pure domain model with no external dependencies. Contains domain entities, state machines, and events.
- **mediator**: Application layer with API, database, and messaging. Orchestrates domain logic via mediator classes.
- **openapi**: API contract definitions. Models are generated from `saksbehandling-api.yaml` using Fabrikt plugin.
- **streams-consumer**: Kafka Streams consumers for external data.

### Domain Model Hierarchy

The domain follows Norwegian terminology. Key aggregate relationships:

```
Person ← SakHistorikk → Sak* → Behandling* → Oppgave (0..1)
                                    ↓
                              UtløstAvType (SØKNAD|MELDEKORT|MANUELL|OMGJØRING|INNSENDING|KLAGE)
```

- **Sak** (Case): Contains multiple behandlinger, identified by `sakId`
- **SakHistorikk**: Groups all saker for a person. Used for `knyttTilSak()` to link new behandlinger to existing saker via `behandlingskjedeId` or `basertPåBehandling`
- **Behandling** (Treatment): A specific processing instance, typed by `UtløstAvType`
- **Oppgave** (Task): Work item with state machine. Always belongs to a Behandling
- **KlageBehandling**: Separate entity for appeal workflows (not a Behandling subclass)
- **Innsending/Utsending**: Document submission/distribution handling

### Oppgave State Machine

All 11 states in `Oppgave.Tilstand.Type`:

```
Opprettet → KlarTilBehandling → UnderBehandling → FerdigBehandlet
                  ↑     ↑            ↓    ↓
                  │     └── PåVent ←─┘    └→ KlarTilKontroll → UnderKontroll
                  │                                  ↑              ↓
                  └──────────────────────────────────┼── returner ──┘
                                                     └── fjernAnsvar
```

Additional locking states: `AvventerLåsAvBehandling` and `AvventerOpplåsingAvBehandling` (used during behandling lock/unlock).
Terminal states: `FerdigBehandlet`, `Avbrutt`, `AvbruttMaskinelt`. Most states can transition to `Avbrutt` via `BehandlingAvbrutt`.

### Event-Driven Architecture

Uses NAV's **Rapids and Rivers** pattern for Kafka messaging:
- **Mottak classes** implement `River.PacketListener` to consume events filtered by `@event_name`
- **Behovløsere** implement the request/response behov pattern
- All mottak/behovløsere are wired in `ApplicationBuilder.kt` — instantiation auto-registers via `River(rapidsConnection).apply(rapidFilter).register(this)` in `init`

#### Adding a New Mottak

Follow the `BehandlingAvbruttMottak` pattern (simplest example):
1. Create class implementing `River.PacketListener` with `rapidFilter` in companion object
2. Use `precondition` for `@event_name` filtering, `validate` for required keys
3. Delegate to appropriate mediator in `onPacket`
4. Instantiate in `ApplicationBuilder.kt` — registration is automatic

#### Behov (Need/Solution Pattern)

Mediators publish `behov` messages with `@behov` specifying what's needed. Behovløsere listen and add solutions. Common behov: `MeldingOmVedtakProdusent`, `Innsending`, `OversendKlageinstans`.

### Mediator Pattern

Seven primary mediators orchestrate domain operations:
- `SakMediator` — Case lifecycle and `knyttTilSak()` overloads per hendelse type
- `OppgaveMediator` — Task workflow (create, assign, complete, abort)
- `UtsendingMediator` — Document distribution
- `InnsendingMediator` — Submission processing
- `KlageMediator` — Appeal handling
- `PersonMediator` — Person data sync (PDL lookup, skjerming)
- `MeldingOmVedtakMediator` — Verdict message HTML generation

### Emneknagger (Tags)

Oppgaver are categorized via `emneknagger: Set<String>`. `EmneknaggBuilder` in `mediator/mottak/` parses Kafka event JSON to derive tags from rettigheter and vilkår data. Custom emneknagger can also be set directly on Oppgave construction.

## Conventions

### Testing

- **JUnit 5** with **Kotest** assertions (`shouldBe`, `shouldThrow`, etc.)
- **MockK** for mocking
- `TestRapid` for testing Kafka message flows — send JSON and verify mediator calls
- Test helpers in `testFixtures` source set: `ModellTestHelper.lagOppgave()`, etc.
- Database tests: `DBTestHelper.withMigratedDb { ds -> ... }` (uses Testcontainers PostgreSQL)

### Code Style

- ktlint enforced automatically (runs `ktlintFormat` before compilation)
- All domain entities use Norwegian naming
- State transitions logged via `Tilstandslogg`
- Events extend `Hendelse` sealed class (~37 subclasses); ansvar-related events extend `AnsvarHendelse`

### OpenAPI Code Generation

Models are generated from `openapi/src/main/resources/saksbehandling-api.yaml`:
- Generated classes have `DTO` suffix (e.g., `OppgaveDTO`, `BehandlingTypeDTO`)
- Fabrikt plugin generates on compilation — no separate command needed
- Generated code is in `openapi/build/generated/`
- DTO mapping lives in `OppgaveDTOMapper.kt` (maps domain → API types)

### Database

- PostgreSQL with Flyway migrations in `mediator/src/main/resources/db/migration/`
- Repository pattern: interfaces in `modell`, implementations as `PostgresXxxRepository` in `mediator`
- `UtløstAvType` is stored as text in `behandling_v1.utlost_av` column

### Kotlin & Build

- Kotlin JVM toolchain **21** (Temurin via `.sdkmanrc`)
- All modules apply `common` convention plugin from `buildSrc/` (configures Kotlin, ktlint, JUnit Platform, parallel test execution)
- `streams-consumer` uses Avro plugin for Kafka schema code generation
- Gradle configuration cache and parallel builds enabled
