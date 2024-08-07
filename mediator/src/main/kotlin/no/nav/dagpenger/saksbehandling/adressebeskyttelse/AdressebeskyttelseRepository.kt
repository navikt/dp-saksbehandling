package no.nav.dagpenger.saksbehandling.adressebeskyttelse

import no.nav.dagpenger.saksbehandling.AdresseBeskyttelseGradering

internal interface AdressebeskyttelseRepository {
    fun oppdaterAdressebeskyttetStatus(
        fnr: String,
        adresseBeskyttelseGradering: AdresseBeskyttelseGradering,
    ): Int

    fun eksistererIDPsystem(fnrs: Set<String>): Set<String>
}
