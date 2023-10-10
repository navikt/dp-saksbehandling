package no.nav.dagpenger.behandling

data class Saksbehandler(val ident: String, private val roller: List<Rolle> = emptyList()) {
    fun harRolle(rolle: Rolle) = roller.contains(rolle)
}
