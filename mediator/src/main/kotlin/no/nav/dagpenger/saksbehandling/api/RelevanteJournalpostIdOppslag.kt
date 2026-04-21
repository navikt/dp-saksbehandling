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
        when (oppgave.behandling.utløstAv) {
            is HendelseBehandler.Intern.Klage -> return coroutineScope {
                val journalpostIderKlage: String? =
                    klageRepository.hentKlageBehandling(oppgave.behandling.behandlingId).journalpostId()
                val journalpostMeldingOmVedtak =
                    utsendingRepository.finnUtsendingForBehandlingId(oppgave.behandling.behandlingId)?.journalpostId()
                (setOf(journalpostIderKlage) + journalpostMeldingOmVedtak).filterNotNull().toSet()
            }

            is HendelseBehandler.Intern.Innsending -> return coroutineScope {
                val journalpostIdInnsending: String? =
                    innsendingRepository.hent(oppgave.behandling.behandlingId).journalpostId
                setOf(journalpostIdInnsending).filterNotNull().toSet()
            }

            is HendelseBehandler.Intern.Oppfølging -> return emptySet()

            is HendelseBehandler.DpBehandling.Søknad ->
                return coroutineScope {
                    val journalpostIderSøknad = async { journalpostIdKlient.hentJournalPostIder(oppgave) }
                    val journalpostMeldingOmVedtak =
                        utsendingRepository.finnUtsendingForBehandlingId(oppgave.behandling.behandlingId)?.journalpostId()
                    (journalpostIderSøknad.await() + journalpostMeldingOmVedtak).filterNotNull().toSet()
                }

            is HendelseBehandler.DpBehandling -> return emptySet()
        }
    }

    private suspend fun JournalpostIdKlient.hentJournalPostIder(oppgave: Oppgave): Set<String> =
        oppgave
            .søknadId()
            ?.let {
                this.hentJournalpostIder(it, oppgave.personIdent())
            }?.getOrNull()
            ?.toSortedSet() ?: emptySet()
}
