package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.hendelser.SøknadInnsendtHendelse

data class Person(val ident: String) {
    private val saker = mutableSetOf<Sak>()

    init {
        require(ident.matches(Regex("\\d{11}"))) { "personident må ha 11 siffer, fikk ${ident.length}" }
    }

    companion object {
        fun rehydrer(
            ident: String,
            saker: Set<Sak>,
        ) = Person(ident).also {
            it.saker.addAll(saker)
        }
    }

    fun hentGjeldendeSak(): Sak {
        return saker.first()
    }

    fun håndter(hendelse: SøknadInnsendtHendelse) {
        if (saker.isEmpty()) {
            saker.add(Sak())
        }
    }

    fun accept(visitor: PersonVisitor) {
        visitor.visit(this.saker)
    }
}
