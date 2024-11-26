package no.nav.dagpenger.saksbehandling.api

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.journalpostid.JournalpostIdClient
import no.nav.dagpenger.saksbehandling.utsending.db.UtsendingRepository

private val sikkerlogg = KotlinLogging.logger("tjenestekall")

internal class RelevanteJournalpostIdOppslag(
    private val journalpostIdClient: JournalpostIdClient,
    private val utsendingRepository: UtsendingRepository,
) {
    suspend fun hentJournalpostIder(oppgave: Oppgave): Set<String> {
        return coroutineScope {
            val journalpostIderSøknad = async { journalpostIdClient.hentJournalPostIder(oppgave.behandling) }
            val journalpostMeldingOmVedtak =
                async { utsendingRepository.finnUtsendingFor(oppgave.oppgaveId)?.journalpostId() }.also {
                    withLoggingContext(
                        "oppgaveId" to oppgave.oppgaveId.toString(),
                        "behandlingId" to oppgave.behandling.behandlingId.toString(),
                    ) {
                        sikkerlogg.debug {
                            "For oppgave med tilstand ${oppgave.tilstand().type}, " +
                                "hentes journalpostId for melding om vedtak: $it"
                        }
                    }
                }
            (journalpostIderSøknad.await() + journalpostMeldingOmVedtak.await()).filterNotNull().toSet()
        }
    }

    private suspend fun JournalpostIdClient.hentJournalPostIder(behandling: Behandling): Set<String> {
        return when (val hendelse = behandling.hendelse) {
            is SøknadsbehandlingOpprettetHendelse -> {
                this.hentJournalpostId(hendelse.søknadId).map {
                    setOf(it)
                }.getOrElse {
                    emptySet()
                }
            }

            else -> emptySet()
        }
    }
}
