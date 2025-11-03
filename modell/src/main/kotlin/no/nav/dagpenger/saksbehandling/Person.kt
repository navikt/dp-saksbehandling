package no.nav.dagpenger.saksbehandling

import java.util.UUID

data class Person(
    val id: UUID = UUIDv7.ny(),
    val ident: String,
    val skjermesSomEgneAnsatte: Boolean,
    val adressebeskyttelseGradering: AdressebeskyttelseGradering,
) {
    init {
        require(ident.matches(Regex("[0-9]{11}"))) { "Person-ident må ha 11 siffer, fikk ${ident.length}" }
    }

    override fun toString(): String {
        return "Person(id=$id, skjermesSomEgneAnsatte=$skjermesSomEgneAnsatte, adressebeskyttelseGradering=$adressebeskyttelseGradering)"
    }
}

enum class AdressebeskyttelseGradering {
    STRENGT_FORTROLIG_UTLAND,
    STRENGT_FORTROLIG,
    FORTROLIG,
    UGRADERT,
}
