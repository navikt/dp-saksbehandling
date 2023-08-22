package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.hendelser.SøknadInnsendtHendelse

data class Person(val ident: String) {
    private val saker = mutableSetOf<Sak>()

    init {
        require(ident.matches(Regex("\\d{11}"))) { "personident må ha 11 siffer, fikk ${ident.length}" }
    }

    fun hentGjeldendeSak(): Sak {
        return saker.first()
    }

    fun håndter(hendelse: SøknadInnsendtHendelse) {
        saker.add(Sak())
    }
}
