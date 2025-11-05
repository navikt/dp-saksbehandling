package no.nav.dagpenger.saksbehandling.tilgangsstyring

open class ManglendeTilgang(
    message: String,
) : RuntimeException(message)

class SaksbehandlerErIkkeEier(
    message: String,
) : ManglendeTilgang(message)
