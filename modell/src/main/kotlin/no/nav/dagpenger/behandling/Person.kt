package no.nav.dagpenger.behandling

class Person(ident: String) {
    private val behandlinger = mutableListOf<Behandling>()

    fun håndter(søknadHendelse: SøknadHendelse) {
        val behandling = NyRettighetsbehandling()
        behandlinger.add(behandling)
        behandling.håndter(søknadHendelse)
    }

    fun harBehandlinger() = this.behandlinger.isNotEmpty()
}
