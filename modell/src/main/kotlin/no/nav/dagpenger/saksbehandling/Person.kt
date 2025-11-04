package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.FORTROLIG
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.STRENGT_FORTROLIG
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand
import no.nav.dagpenger.saksbehandling.TilgangType.EGNE_ANSATTE
import no.nav.dagpenger.saksbehandling.TilgangType.FORTROLIG_ADRESSE
import no.nav.dagpenger.saksbehandling.TilgangType.STRENGT_FORTROLIG_ADRESSE
import no.nav.dagpenger.saksbehandling.TilgangType.STRENGT_FORTROLIG_ADRESSE_UTLAND
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

    fun egneAnsatteTilgangskontroll(saksbehandler: Saksbehandler) {
        if (!this.skjermesSomEgneAnsatte) {
            return
        }
        require(saksbehandler.tilganger.contains(EGNE_ANSATTE)) {
            throw Oppgave.Tilstand.IkkeTilgangTilEgneAnsatte("Saksbehandler har ikke tilgang til egne ansatte")
        }
    }

    fun adressebeskyttelseTilgangskontroll(saksbehandler: Saksbehandler) {
        val adressebeskyttelseGradering = this.adressebeskyttelseGradering
        require(
            when (adressebeskyttelseGradering) {
                FORTROLIG -> saksbehandler.tilganger.contains(FORTROLIG_ADRESSE)
                STRENGT_FORTROLIG -> saksbehandler.tilganger.contains(STRENGT_FORTROLIG_ADRESSE)
                STRENGT_FORTROLIG_UTLAND -> saksbehandler.tilganger.contains(STRENGT_FORTROLIG_ADRESSE_UTLAND)
                UGRADERT -> true
            },
        ) {
            throw Tilstand.ManglendeTilgangTilAdressebeskyttelse(
                "Saksbehandler mangler tilgang til adressebeskyttede personer. Adressebeskyttelse: $adressebeskyttelseGradering",
            )
        }
    }
}

enum class AdressebeskyttelseGradering {
    STRENGT_FORTROLIG_UTLAND,
    STRENGT_FORTROLIG,
    FORTROLIG,
    UGRADERT,
}
