package no.nav.dagpenger.saksbehandling.api

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import mu.KotlinLogging
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.journalpostid.JournalpostIdClient
import no.nav.dagpenger.saksbehandling.utsending.db.UtsendingRepository

private val sikkerlogg = KotlinLogging.logger("tjenestekall")

class RelevanteJournalpostIdOppslag(
    private val journalpostIdClient: JournalpostIdClient,
    private val utsendingRepository: UtsendingRepository,
) {
    suspend fun hentJournalpostIder(oppgave: Oppgave): Set<String> {
        return coroutineScope {
            val journalpostIderSøknad = async { journalpostIdClient.hentJournalPostIder(oppgave.behandling) }
            val journalpostMeldingOmVedtak =
                async { utsendingRepository.finnUtsendingFor(oppgave.oppgaveId)?.journalpostId() }
            (journalpostIderSøknad.await() + journalpostMeldingOmVedtak.await()).filterNotNull().toSet()
        }
    }

    private suspend fun JournalpostIdClient.hentJournalPostIder(behandling: Behandling): Set<String> {
        return when (val hendelse = behandling.hendelse) {
            is SøknadsbehandlingOpprettetHendelse -> {
                this.hentJournalpostIder(hendelse.søknadId, behandling.person.ident).getOrNull()?.toSortedSet() ?: emptySet()
            }
            else -> emptySet()
        }
    }
}
