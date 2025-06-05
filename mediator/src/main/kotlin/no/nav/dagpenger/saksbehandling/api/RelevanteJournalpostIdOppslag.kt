package no.nav.dagpenger.saksbehandling.api

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.Behandling
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
        when (oppgave.behandling.type) {
            BehandlingType.KLAGE -> return coroutineScope {
                val journalpostIderKlage: String? = klageRepository.hentKlageBehandling(oppgave.behandling.behandlingId).journalpostId()
                val journalpostMeldingOmVedtak =
                    utsendingRepository.finnUtsendingFor(oppgave.oppgaveId)?.journalpostId()
                (setOf(journalpostIderKlage) + journalpostMeldingOmVedtak).filterNotNull().toSet()
            }
            BehandlingType.RETT_TIL_DAGPENGER ->
                return coroutineScope {
                    val journalpostIderSøknad = async { journalpostIdKlient.hentJournalPostIder(oppgave.behandling) }
                    val journalpostMeldingOmVedtak =
                        utsendingRepository.finnUtsendingFor(oppgave.oppgaveId)?.journalpostId()
                    (journalpostIderSøknad.await() + journalpostMeldingOmVedtak).filterNotNull().toSet()
                }
            BehandlingType.MELDEKORT -> return emptySet()
        }
    }

    private suspend fun JournalpostIdKlient.hentJournalPostIder(behandling: Behandling): Set<String> {
        return when (val hendelse = behandling.hendelse) {
            is SøknadsbehandlingOpprettetHendelse -> {
                this.hentJournalpostIder(hendelse.søknadId, behandling.person.ident).getOrNull()?.toSortedSet() ?: emptySet()
            }
            else -> emptySet()
        }
    }
}
