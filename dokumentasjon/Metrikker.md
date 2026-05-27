# Metrikker

Oversikt over alle Prometheus-metrikker eksponert av dp-saksbehandling.

## Forretningsmetrikker

| Metric | Type | Labels | Beskrivelse | Kilde |
|--------|------|--------|-------------|-------|
| `dp_saksbehandling_oppgave_tilstand_gauge` | Gauge | `tilstand` | Antall oppgaver i hver tilstand | `OppgaveTilstandMetrikker` |
| `dp_saksbehandling_oppgave_tilstand_siste_24_t_gauge` | Gauge | `tilstand` | Antall oppgaver i hver tilstand siste 24t | `OppgaveTilstandSiste24TimerMetrikker` |
| `dp_saksbehandling_utsending_tilstand_gauge` | Gauge | `tilstand` | Antall utsendinger i hver tilstand | `UtsendingTilstandMetrikker` |

## API-metrikker

| Metric | Type | Labels | Beskrivelse | Kilde |
|--------|------|--------|-------------|-------|
| `dp_saksbehandling_oppgave_api_feil` | Counter | `feiltype` | Antall feil kastet i oppgave-APIet | `StatusPages` |

## Integrasjonsmetrikker

| Metric | Type | Labels | Beskrivelse | Kilde |
|--------|------|--------|-------------|-------|
| `dp_saksbehandling_skjerming_oppdateringer` | Counter | `status` | Antall oppdateringer av skjermingsstatus | `SkjermingConsumer` |
| `dp_saksbehandling_adressebeskyttelse_oppdateringer` | Counter | `status` | Antall oppdateringer av adressebeskyttelsestatus | `AdressebeskyttelseConsumer` |
| `dp_saksbehandling_saksbehandler_oppslag_cache` | Counter | `treff` | Cache-treff på saksbehandleroppslag | `SaksbehandlerOppslag` |
| `dp_saksbehandling_saksbehandler_oppslag_duration` | Histogram | — | Tid brukt på oppslag av saksbehandler | `SaksbehandlerOppslag` |

## Databasemetrikker

| Metric | Type | Labels | Beskrivelse | Kilde |
|--------|------|--------|-------------|-------|
| `transactions_committed_total` | Counter | — | Totalt antall committede transaksjoner | `DbMetrics` |
| `transactions_rolledback_total` | Counter | — | Totalt antall rollback-ede transaksjoner | `DbMetrics` |
| `transaction_duration_seconds` | Histogram | — | Full transaksjonsvarighet inkludert queries og commit | `DbMetrics` |
| `commit_duration_seconds` | Histogram | — | Tid brukt på selve DB-commit | `DbMetrics` |
| `active_transactions` | Gauge | — | Antall aktive transaksjoner akkurat nå | `DbMetrics` |

## Merknader

- Forretningsmetrikker oppdateres av `StatistikkJob` som kjører hvert 5. minutt
- Databasemetrikker mangler `dp_saksbehandling_`-prefiks (teknisk gjeld, lav prioritet)
- HikariCP eksponerer egne metrikker automatisk via Prometheus-integrasjonen
