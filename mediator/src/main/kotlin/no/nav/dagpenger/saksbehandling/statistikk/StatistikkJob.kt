package no.nav.dagpenger.saksbehandling.statistikk

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.saksbehandling.job.Job
import no.nav.dagpenger.saksbehandling.statistikk.db.SaksbehandlingsstatistikkRepository

class StatistikkJob(
    private val rapidsConnection: RapidsConnection,
    private val saksbehandlingsstatistikkRepository: SaksbehandlingsstatistikkRepository,
) : Job() {
    override val jobName: String = "StatistikkJob"
    override val logger: KLogger = KotlinLogging.logger {}

    override suspend fun executeJob() {
        if (saksbehandlingsstatistikkRepository.tidligereTilstandsendringerErOverført()) {
            logger.info { "Starter publisering av oppgavetilstandsendringer til statistikk." }
        } else {
            logger.error { "Ikke alle oppgavetilstandsendringer er publisert til statistikk. Avbryter kjøring." }
            return
        }
        runCatching {
            val oppgaveTilstandsendringer =
                saksbehandlingsstatistikkRepository.oppgaveTilstandsendringer().also {
                    logger.info { "Fant ${it.size} oppgavetilstandsendringer som skal publiseres til statistikk." }
                }
            oppgaveTilstandsendringer.forEach { oppgaveTilstandsendring ->
                rapidsConnection
                    .publish(
                        key = oppgaveTilstandsendring.personIdent,
                        message =
                            JsonMessage
                                .newMessage(
                                    mapOf(
                                        "@event_name" to "oppgave_til_statistikk",
                                        "oppgave" to oppgaveTilstandsendring.asMap(),
                                    ),
                                ).toJson(),
                    ).also {
                        saksbehandlingsstatistikkRepository.markerTilstandsendringerSomOverført(
                            tilstandId = oppgaveTilstandsendring.tilstandsendring.tilstandsendringId,
                        )
                        logger.info {
                            "Publisert oppgavetilstandsendring med id ${oppgaveTilstandsendring.tilstandsendring.tilstandsendringId} til statistikk."
                        }
                    }
            }
            logger.info { "Publisering av oppgavetilstandsendringer til statistikk ferdig." }
        }.onFailure {
            logger.error(it) { "Feil under kjøring av StatistikkJob: $it" }
        }
    }
}
