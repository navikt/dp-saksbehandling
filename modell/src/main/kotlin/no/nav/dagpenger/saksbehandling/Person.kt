package no.nav.dagpenger.saksbehandling

data class Person(val ident: String) {
    init {
        require(ident.matches(Regex("\\d{11}"))) { "Person-ident må ha 11 siffer, fikk ${ident.length}" }
    }
}
