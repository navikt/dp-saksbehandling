package no.nav.dagpenger.saksbehandling.statistikk

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.OutgoingMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.saksbehandling.job.Job
import no.nav.dagpenger.saksbehandling.statistikk.db.SaksbehandlingsstatistikkRepository
import no.nav.dagpenger.saksbehandling.statistikk.db.SaksbehandlingsstatistikkRepository.Companion.ANTALL_PER_KJØRING

class StatistikkJob(
    private val rapidsConnection: RapidsConnection,
    private val saksbehandlingsstatistikkRepository: SaksbehandlingsstatistikkRepository,
) : Job() {
    override val jobName: String = "StatistikkJob"
    override val logger: KLogger = KotlinLogging.logger {}

    private fun OppgaveITilstand.Tilstandsendring.prettyPrint(): String =
        "Tilstandsendring(id=${this.tilstandsendringId}, tidspunkt=${this.tidspunkt})"

    private fun List<OppgaveITilstand>.loggOppgaveTilstandsEndringer() {
        val string = "Fant ${this.size} oppgavetilstandsendringer som skal publiseres til statistikk. "
        when (this.size) {
            0 -> {
                logger.info { string }
            }

            else -> {
                logger.info {
                    string +
                        "Start: ${this.first().tilstandsendring.prettyPrint()} Slutt: ${this.last().tilstandsendring.prettyPrint()}"
                }
            }
        }
    }

    override suspend fun executeJob() {
        val ikkeOverfort =
            saksbehandlingsstatistikkRepository
                .oppgaveTilstandsendringerIkkeOverfort(ANTALL_PER_KJØRING)

        // Materialiser ikke nye kandidater så lenge det ligger uleverte rader. Da kan backloggen
        // aldri vokse forbi én batch, og nye tilstandsendringer blir liggende som kandidater — en
        // langt billigere kø. Sikrer samtidig at gammelt alltid leveres før nytt.
        val oppgaveTilstandsendringer =
            when (ikkeOverfort.isEmpty()) {
                true -> saksbehandlingsstatistikkRepository.oppgaveTilstandsendringer()
                false -> {
                    logger.info {
                        "Fant ${ikkeOverfort.size} uleverte tilstandsendringer, materialiserer ikke nye"
                    }
                    ikkeOverfort
                }
            }

        oppgaveTilstandsendringer.loggOppgaveTilstandsEndringer()

        oppgaveTilstandsendringer.forEach { oppgaveTilstandsendring ->
            val melding =
                OutgoingMessage(
                    body =
                        JsonMessage
                            .newMessage(
                                mapOf(
                                    "@event_name" to "oppgave_til_statistikk_v7",
                                    "oppgave" to oppgaveTilstandsendring.asMap(),
                                ),
                            ).toJson(),
                    key = oppgaveTilstandsendring.personIdent,
                )

            // Synkron send()-feil kastes ut herfra. Async leveransefeil rapporteres i stedet som
            // FailedMessage uten å kaste, og håndteres under.
            val (_, feilet) = rapidsConnection.publish(listOf(melding))
            when (feilet.isEmpty()) {
                true -> {
                    saksbehandlingsstatistikkRepository
                        .markerTilstandsendringerSomOverført(
                            tilstandId = oppgaveTilstandsendring.tilstandsendring.tilstandsendringId,
                        ).let {
                            if (it != 1) {
                                logger.warn {
                                    "Fikk ikke markert tilstandsendring som overført for tilstandsenringId: " +
                                        "${oppgaveTilstandsendring.tilstandsendring.tilstandsendringId}"
                                }
                            }
                        }
                    logger.info {
                        "Publisert oppgavetilstandsendring med " +
                            "id ${oppgaveTilstandsendring.tilstandsendring.tilstandsendringId} til statistikk."
                    }
                }

                false -> {
                    throw IllegalStateException(
                        "Leveransefeil for tilstandsendring ${oppgaveTilstandsendring.tilstandsendring.tilstandsendringId} " +
                            "— stopper (forblir uoverført)",
                        feilet.first().error,
                    )
                }
            }
        }
        logger.info { "Publisering av oppgavetilstandsendringer til statistikk ferdig." }
    }
}
