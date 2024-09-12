package no.nav.dagpenger.saksbehandling.api.tilgangskontroll

data class Saksbehandler(
    val navIdent: String,
    val grupper: Set<String>,
    val token: String,
)
