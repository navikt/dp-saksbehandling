package no.nav.dagpenger.saksbehandling

data class Saksbehandler(
    val navIdent: String,
    val grupper: Set<String>,
    val tilganger: Set<TilgangType> = emptySet(),
) : Behandler

data class Applikasjon(
    val navn: String,
) : Behandler

sealed interface Behandler
