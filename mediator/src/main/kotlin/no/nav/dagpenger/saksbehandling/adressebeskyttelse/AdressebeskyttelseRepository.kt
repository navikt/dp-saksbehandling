package no.nav.dagpenger.saksbehandling.adressebeskyttelse

import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering

internal interface AdressebeskyttelseRepository {
    fun oppdaterAdressebeskyttetStatus(
        fnr: String,
        adresseBeskyttelseGradering: AdressebeskyttelseGradering,
    ): Int

    fun eksistererIDPsystem(fnrs: Set<String>): Set<String>
}
