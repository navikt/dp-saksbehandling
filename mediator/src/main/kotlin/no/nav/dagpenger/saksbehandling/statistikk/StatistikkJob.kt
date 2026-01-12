package no.nav.dagpenger.saksbehandling.statistikk

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
import no.nav.dagpenger.saksbehandling.job.Job
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import java.util.UUID

class StatistikkJob(
    private val rapidsConnection: RapidsConnection,
    private val sakMediator: SakMediator,
    private val statistikkTjeneste: StatistikkTjeneste,
    private val oppgaveRepository: OppgaveRepository,
) : Job() {
    override val jobName: String = "StatistikkJob"
    override val logger: KLogger = KotlinLogging.logger {}

    override suspend fun executeJob() {
        if (statistikkTjeneste.tidligereOppgaverErOverførtTilStatistikk()) {
            logger.info { "Starter publisering av oppgaver til statistikk." }
        } else {
            logger.error { "Ikke alle oppgaver er publisert til statistikk. Avbryter kjøring." }
            return
        }
        val oppgaveIdListe: List<UUID> = statistikkTjeneste.oppgaverTilStatistikk()
        logger.info { "Antall oppgave som skal publiseres til statistikk: ${oppgaveIdListe.size}" }
        val statistikkOppgaver =
            oppgaveIdListe.map {
                val oppgave = oppgaveRepository.hentOppgave(it)
                val sakId = sakMediator.hentSakIdForBehandlingId(oppgave.behandling.behandlingId)
                StatistikkOppgave(
                    oppgave = oppgave,
                    sakId = sakId,
                )
            }
        statistikkOppgaver.forEach { statistikkOppgave ->
            rapidsConnection
                .publish(
                    key = statistikkOppgave.personIdent,
                    message =
                        JsonMessage
                            .newMessage(
                                mapOf(
                                    "@event_name" to "oppgave_til_statistikk",
                                    "oppgave" to statistikkOppgave.asMap(),
                                ),
                            ).toJson(),
                ).also {
                    statistikkTjeneste.markerOppgaveTilStatistikkSomOverført(statistikkOppgave.oppgaveId)
                    logger.info {
                        "Publisert oppgave med id ${statistikkOppgave.oppgaveId} til statistikk."
                    }
                }
        }
        logger.info { "Publisering av oppgaver til statistikk ferdig." }
    }
}
