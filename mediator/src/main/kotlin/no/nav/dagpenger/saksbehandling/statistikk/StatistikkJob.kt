package no.nav.dagpenger.saksbehandling.statistikk

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
import no.nav.dagpenger.saksbehandling.job.Job
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import java.time.LocalDateTime
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
        val oppgaveListe: List<Pair<UUID, LocalDateTime>> = statistikkTjeneste.oppgaverTilStatistikk()
        logger.info { "Antall oppgave som skal publiseres til statistikk: ${oppgaveListe.size}" }
        val oppgaver =
            oppgaveListe.map {
                val oppgave = oppgaveRepository.hentOppgave(it.first)
                val sakId = sakMediator.hentSakIdForBehandlingId(oppgave.behandling.behandlingId)
                StatistikkOppgave(
                    oppgave = oppgave,
                    sakId = sakId,
                )
            }
        oppgaver.forEach { oppgave ->
            rapidsConnection.publish(
                key = oppgave.personIdent,
                message =
                    JsonMessage
                        .newMessage(
                            mapOf(
                                "@event_name" to "oppgave_til_statistikk",
                                "oppgave" to oppgave.asMap(),
                            ),
                        ).toJson(),
            )
        }
        logger.info { "Publisering av oppgaver til statistikk ferdig." }
    }
}
