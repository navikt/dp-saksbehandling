package no.nav.dagpenger.saksbehandling.statistikk

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.oppgave.OppgaveObserver
import no.nav.dagpenger.saksbehandling.sak.SakMediator

private val logger = KotlinLogging.logger { }

class SaksbehandlingStatistikk(
    private val rapidsConnection: RapidsConnection,
    private val saksMediator: SakMediator,
) : OppgaveObserver {
    override fun oppgaveEndret(oppgave: Oppgave) {
        try {
            val statistikkOppgave =
                StatistikkOppgave(
                    oppgave = oppgave,
                    sakId = saksMediator.hentSakIdForBehandlingId(oppgave.behandling.behandlingId),
                )
            rapidsConnection
                .publish(
                    key = statistikkOppgave.personIdent,
                    message =
                        JsonMessage.Companion
                            .newMessage(
                                mapOf(
                                    "@event_name" to "oppgave_til_statistikk_v2",
                                    "oppgave" to statistikkOppgave.asMap(),
                                ),
                            ).toJson(),
                ).also {
                    logger.info { "SaksbehandlingStatistikk  oppgave endret for oppgave: $statistikkOppgave" }
                }
        } catch (e: Exception) {
            logger.error(e) { "Feil ved sending av oppgave til statistikk for oppgaveId=${oppgave.oppgaveId}" }
        }
    }
}
