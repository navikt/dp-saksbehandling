package no.nav.dagpenger.saksbehandling.api

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import no.nav.dagpenger.saksbehandling.HendelseBehandler
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.db.innsending.InnsendingRepository
import no.nav.dagpenger.saksbehandling.db.klage.KlageRepository
import no.nav.dagpenger.saksbehandling.journalpostid.JournalpostIdKlient
import no.nav.dagpenger.saksbehandling.utsending.db.UtsendingRepository

class RelevanteJournalpostIdOppslag(
    private val journalpostIdKlient: JournalpostIdKlient,
    private val utsendingRepository: UtsendingRepository,
    private val klageRepository: KlageRepository,
    private val innsendingRepository: InnsendingRepository,
) {
    suspend fun hentJournalpostIder(oppgave: Oppgave): Set<String> {
        val journalpostForUtsending =
            utsendingRepository.finnUtsendingForBehandlingId(oppgave.behandling.behandlingId)?.journalpostId()
        val journalposter: Set<String> =
            when (oppgave.behandling.utløstAv) {
                is HendelseBehandler.Intern.Klage ->
                    coroutineScope {
                        val journalpostIderKlage: String? =
                            klageRepository.hentKlageBehandling(oppgave.behandling.behandlingId).journalpostId()

                        (setOf(journalpostIderKlage)).filterNotNull().toSet()
                    }

                is HendelseBehandler.Intern.Innsending ->
                    coroutineScope {
                        val journalpostIdInnsending: String =
                            innsendingRepository.hent(oppgave.behandling.behandlingId).journalpostId
                        (setOf(journalpostIdInnsending)).toSet()
                    }

                is HendelseBehandler.DpBehandling.Søknad ->
                    coroutineScope {
                        val journalpostIderSøknad = async { journalpostIdKlient.hentJournalPostIder(oppgave) }
                        (journalpostIderSøknad.await()).toSet()
                    }
                else -> journalpostForUtsending?.let { setOf(it) } ?: emptySet()
            }
        return journalposter + setOfNotNull(journalpostForUtsending)
    }

    private suspend fun JournalpostIdKlient.hentJournalPostIder(oppgave: Oppgave): Set<String> =
        oppgave
            .søknadId()
            ?.let {
                this.hentJournalpostIder(it, oppgave.personIdent())
            }?.getOrNull()
            ?.toSortedSet() ?: emptySet()
}
