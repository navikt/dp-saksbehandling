# dp-saksbehandling

Backend-applikasjon som lytter på behandlingshendelser og håndterer oppgavebehandling knyttet til dagpenger.

## Domene: Ferietillegg

`Ferietillegg` er en egen behandlingstype i saksbehandling (`FERIETILLEGG`), ikke bare en emneknagg.

- En sak regnes som ferietilleggsak dersom den har minst én behandling utløst av `Ferietillegg`.
- Saksoversikt skiller derfor mellom vanlige dagpengesaker og `ferietilleggSaker`.
- I API eksponeres dette både som eget felt (`ferietilleggSaker`) i personoversikt og som `utlostAv = FERIETILLEGG` på oppgaver.
- Emneknagg-bygging setter også "Ferietillegg" som behandlet hendelsestype når underliggende hendelse har type `Ferietillegg`.

## API dokumentasjon

Link til swagger api dokumentasjon: https://dp-saksbehandling.intern.dev.nav.no/openapi

## Komme i gang

Gradle brukes som byggverktøy og er bundlet inn.

`./gradlew build`

# Henvendelser

Spørsmål knyttet til koden kan rettes til:
* #team-dagpenger-behandling på Slack
* Eller en annen måte for omverden å kontakte teamet på

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #team-dagpenger-behandling.
