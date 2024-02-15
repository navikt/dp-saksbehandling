package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpprettetHendelse

data class Person(val ident: String) {
    val behandlinger = mutableSetOf<Behandling>()

    init {
        require(ident.matches(Regex("\\d{11}"))) { "personident må ha 11 siffer, fikk ${ident.length}" }
    }

    fun håndter(behandlingOpprettetHendelse: BehandlingOpprettetHendelse) {
        val behandling = Behandling(behandlingOpprettetHendelse.behandlingId)
        behandlinger.add(behandling)
    }
}
