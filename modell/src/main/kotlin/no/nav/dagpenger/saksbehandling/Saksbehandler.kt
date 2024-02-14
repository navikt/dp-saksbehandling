package no.nav.dagpenger.saksbehandling

data class Saksbehandler(val ident: String, private val roller: List<Rolle> = emptyList()) {
    fun harRolle(rolle: Rolle) = roller.contains(rolle)
}
