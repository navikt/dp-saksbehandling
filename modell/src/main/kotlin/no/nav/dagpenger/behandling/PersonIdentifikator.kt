package no.nav.dagpenger.behandling

class PersonIdentifikator(private val ident: String) {

    init {
        require(ident.matches(Regex("\\d{11}"))) { "personident må ha 11 siffer" }
    }

    companion object {
        fun String.tilPersonIdentfikator() = PersonIdentifikator(this)
    }

    override fun equals(other: Any?): Boolean = other is PersonIdentifikator && other.ident == this.ident
}
