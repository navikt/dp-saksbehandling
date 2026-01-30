package no.nav.dagpenger.saksbehandling.statistikk

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.saksbehandling.job.Job

class StatistikkJob(
    private val rapidsConnection: RapidsConnection,
    private val statistikkTjeneste: StatistikkTjeneste,
) : Job() {
    override val jobName: String = "StatistikkJob"
    override val logger: KLogger = KotlinLogging.logger {}

    override suspend fun executeJob() {
        if (statistikkTjeneste.tidligereTilstandsendringErOverført()) {
            logger.info { "Starter publisering av oppgaver til statistikk." }
        } else {
            logger.error { "Ikke alle oppgaver er publisert til statistikk. Avbryter kjøring." }
            return
        }
        val oppgaveTilstandsendringer = statistikkTjeneste.oppgaveTilstandsendringer()
        oppgaveTilstandsendringer.forEach { oppgaveTilstandsendring ->
            rapidsConnection
                .publish(
                    key = oppgaveTilstandsendring.personIdent,
                    message =
                        JsonMessage
                            .newMessage(
                                mapOf(
                                    "@event_name" to "oppgave_til_statistikk_v3",
                                    "oppgave" to oppgaveTilstandsendring.asMap(),
                                ),
                            ).toJson(),
                ).also {
//                    statistikkTjeneste.markerOppgaveTilStatistikkSomOverført(oppgaveTilstandsendring.oppgaveId)
                    logger.info {
                        "Publisert oppgave med id ${oppgaveTilstandsendring.oppgaveId} til statistikk."
                    }
                }
        }
        logger.info { "Publisering av oppgaver til statistikk ferdig." }
    }
}
