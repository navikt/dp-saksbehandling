package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.FORTROLIG
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.STRENGT_FORTROLIG
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.TilgangType.EGNE_ANSATTE
import no.nav.dagpenger.saksbehandling.TilgangType.FORTROLIG_ADRESSE
import no.nav.dagpenger.saksbehandling.TilgangType.STRENGT_FORTROLIG_ADRESSE
import no.nav.dagpenger.saksbehandling.TilgangType.STRENGT_FORTROLIG_ADRESSE_UTLAND
import no.nav.dagpenger.saksbehandling.tilgangsstyring.ManglendeTilgang
import java.util.UUID

data class Person(
    val id: UUID = UUIDv7.ny(),
    val ident: String,
    val skjermesSomEgneAnsatte: Boolean,
    val adressebeskyttelseGradering: AdressebeskyttelseGradering,
    val inhabileNavIdenter: List<String>,
) {
    init {
        require(ident.matches(Regex("[0-9]{11}"))) { "Person-ident må ha 11 siffer, fikk ${ident.length}" }
    }

    override fun toString(): String =
        "Person(id=$id, skjermesSomEgneAnsatte=$skjermesSomEgneAnsatte, adressebeskyttelseGradering=$adressebeskyttelseGradering)"

    fun harTilgang(saksbehandler: Saksbehandler) {
        egneAnsatteTilgangskontroll(saksbehandler)
        adressebeskyttelseTilgangskontroll(saksbehandler)
        habilitetTilgangskontroll(saksbehandler)
    }

    fun egneAnsatteTilgangskontroll(saksbehandler: Saksbehandler) {
        if (!this.skjermesSomEgneAnsatte) {
            return
        }
        require(saksbehandler.tilganger.contains(EGNE_ANSATTE)) {
            throw IkkeTilgangTilEgneAnsatte("Saksbehandler(${saksbehandler.navIdent}) har ikke tilgang til egne ansatte(${this.id})")
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
            throw ManglendeTilgangTilAdressebeskyttelse(
                "Saksbehandler(${saksbehandler.navIdent}) mangler tilgang til adressebeskyttet person(${this.id}). Adressebeskyttelse: $adressebeskyttelseGradering",
            )
        }
    }

    fun habilitetTilgangskontroll(saksbehandler: Saksbehandler) {
        require(!inhabileNavIdenter.contains(saksbehandler.navIdent)) {
            throw Inhabil("Saksbehandler(${saksbehandler.navIdent}) er inhabil for denne personen(${this.id})")
        }
    }

    /**
     * Registrerer at [navIdent] er inhabil for denne personen. Inhabilitet er permanent og kan ikke fjernes.
     */
    fun registrerInhabilitet(navIdent: String): Person = copy(inhabileNavIdenter = (inhabileNavIdenter + navIdent).distinct())
}

enum class AdressebeskyttelseGradering {
    STRENGT_FORTROLIG_UTLAND,
    STRENGT_FORTROLIG,
    FORTROLIG,
    UGRADERT,
}

class IkkeTilgangTilEgneAnsatte(
    message: String,
) : ManglendeTilgang(message)

class ManglendeTilgangTilAdressebeskyttelse(
    message: String,
) : ManglendeTilgang(message)

class Inhabil(
    message: String,
) : ManglendeTilgang(message)
