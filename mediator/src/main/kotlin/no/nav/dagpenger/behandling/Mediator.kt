package no.nav.dagpenger.behandling

class Mediator {

    fun hentBehandlinger(): List<Behandling> {
        return listOf(Hubba.bubba())
    }

    fun hentBehandling(oppgaveId: String): Behandling {
        return Hubba.bubba()
    }
}
