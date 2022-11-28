package no.nav.dagpenger.behandling

class Person(ident: String) {
    private val saker = mutableListOf<Sak>()
    fun håndter(søknadHendelse: SøknadHendelse) {
        val sak = Sak()
        saker.add(sak)
        sak.håndter(søknadHendelse)
    }

    fun harSaker() = this.saker.isNotEmpty()
}
