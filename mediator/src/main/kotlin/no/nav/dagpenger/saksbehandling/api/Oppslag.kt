package no.nav.dagpenger.saksbehandling.api

import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.Oppgave
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
    suspend fun hentPerson(ident: String): PDLPersonIntern {
        return pdlKlient.person(ident).getOrThrow()
    }

    suspend fun hentJournalpostIder(oppgave: Oppgave): Set<String> {
        return relevanteJournalpostIdOppslag.hentJournalpostIder(oppgave)
    }

    suspend fun hentBehandler(ident: String): BehandlerDTO {
        return saksbehandlerOppslag.hentSaksbehandler(ident)
    }

    suspend fun erSkjermetPerson(ident: String): Boolean {
        return skjermingKlient.erSkjermetPerson(ident).getOrThrow()
    }

    suspend fun erAdressebeskyttetPerson(ident: String): AdressebeskyttelseGradering {
        return pdlKlient.person(ident).getOrThrow().adresseBeskyttelseGradering
    }
}
