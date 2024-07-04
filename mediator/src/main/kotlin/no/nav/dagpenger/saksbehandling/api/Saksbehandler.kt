package no.nav.dagpenger.saksbehandling.api

data class Saksbehandler(
    val navIdent: String,
    val grupper: Set<String>,
)
