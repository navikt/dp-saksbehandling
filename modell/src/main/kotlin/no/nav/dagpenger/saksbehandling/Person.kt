package no.nav.dagpenger.saksbehandling

import java.util.UUID

data class Person(
    val id: UUID = UUIDv7.ny(),
    val ident: String,
    val skjermesSomEgneAnsatte: Boolean,
    val adressebeskyttelseGradering: AdressebeskyttelseGradering,
) {
    init {
        require(ident.matches(Regex("\\d{11}"))) { "Person-ident må ha 11 siffer, fikk ${ident.length}" }
    }
}

enum class AdressebeskyttelseGradering {
    STRENGT_FORTROLIG_UTLAND,
    STRENGT_FORTROLIG,
    FORTROLIG,
    UGRADERT,
}
