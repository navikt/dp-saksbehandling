package no.nav.dagpenger.saksbehandling.api

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.api.models.BehandlerDTO
import no.nav.dagpenger.saksbehandling.pdl.PDLKlient
import no.nav.dagpenger.saksbehandling.pdl.PDLPersonIntern
import no.nav.dagpenger.saksbehandling.saksbehandler.SaksbehandlerOppslag
import no.nav.dagpenger.saksbehandling.skjerming.SkjermingKlient

class Oppslag(
    private val pdlKlient: PDLKlient,
    private val relevanteJournalpostIdOppslag: RelevanteJournalpostIdOppslag,
    private val saksbehandlerOppslag: SaksbehandlerOppslag,
    private val skjermingKlient: SkjermingKlient,
) {
    suspend fun hentPerson(ident: String): PDLPersonIntern = pdlKlient.person(ident).getOrThrow()

    suspend fun hentJournalpostIder(oppgave: Oppgave): Set<String> = relevanteJournalpostIdOppslag.hentJournalpostIder(oppgave)

    suspend fun hentBehandler(ident: String): BehandlerDTO = saksbehandlerOppslag.hentSaksbehandler(ident)

    suspend fun erSkjermetPerson(ident: String): Boolean = skjermingKlient.erSkjermetPerson(ident).getOrThrow()

    suspend fun adressebeskyttelseGradering(ident: String): AdressebeskyttelseGradering =
        pdlKlient.person(ident).getOrThrow().adresseBeskyttelseGradering

    suspend fun hentPersonMedSkjermingOgAdressebeskyttelse(ident: String): Person =
        coroutineScope {
            val skjermesSomEgneAnsatte =
                async {
                    erSkjermetPerson(ident)
                }

            val adresseBeskyttelseGradering =
                async {
                    adressebeskyttelseGradering(ident)
                }

            Person(
                ident = ident,
                skjermesSomEgneAnsatte = skjermesSomEgneAnsatte.await(),
                adressebeskyttelseGradering = adresseBeskyttelseGradering.await(),
            )
        }
}
