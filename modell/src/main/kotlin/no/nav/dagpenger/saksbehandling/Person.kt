package no.nav.dagpenger.saksbehandling

data class Person(val ident: String) {
    init {
        require(ident.matches(Regex("\\d{11}"))) { "personident må ha 11 siffer, fikk ${ident.length}" }
    }
}
