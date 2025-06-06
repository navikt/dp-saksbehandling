package no.nav.dagpenger.saksbehandling.api

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.BehandlingType
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.db.klage.KlageRepository
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.journalpostid.JournalpostIdKlient
import no.nav.dagpenger.saksbehandling.utsending.db.UtsendingRepository

private val sikkerlogg = KotlinLogging.logger("tjenestekall")

class RelevanteJournalpostIdOppslag(
    private val journalpostIdKlient: JournalpostIdKlient,
    private val utsendingRepository: UtsendingRepository,
    private val klageRepository: KlageRepository,
) {
    suspend fun hentJournalpostIder(oppgave: Oppgave): Set<String> {
        when (oppgave.behandlingType) {
            BehandlingType.KLAGE -> return coroutineScope {
                val journalpostIderKlage: String? =
                    klageRepository.hentKlageBehandling(oppgave.behandlingId).journalpostId()
                val journalpostMeldingOmVedtak =
                    utsendingRepository.finnUtsendingFor(oppgave.oppgaveId)?.journalpostId()
                (setOf(journalpostIderKlage) + journalpostMeldingOmVedtak).filterNotNull().toSet()
            }

            BehandlingType.RETT_TIL_DAGPENGER ->
                return coroutineScope {
                    val journalpostIderSøknad = async { journalpostIdKlient.hentJournalPostIder(oppgave) }
                    val journalpostMeldingOmVedtak =
                        utsendingRepository.finnUtsendingFor(oppgave.oppgaveId)?.journalpostId()
                    (journalpostIderSøknad.await() + journalpostMeldingOmVedtak).filterNotNull().toSet()
                }

            BehandlingType.MELDEKORT -> return emptySet()
        }
    }

    private suspend fun JournalpostIdKlient.hentJournalPostIder(oppgave: Oppgave): Set<String> {
        val søknadId =
            oppgave.tilstandslogg.find { it.hendelse is SøknadsbehandlingOpprettetHendelse }?.hendelse.let {
                when (it) {
                    is SøknadsbehandlingOpprettetHendelse -> it.søknadId
                    else -> null
                }
            }

        return if (søknadId != null) {
            this.hentJournalpostIder(søknadId, oppgave.personIdent()).getOrNull()?.toSortedSet() ?: emptySet()
        } else {
            emptySet()
        }
    }
}
