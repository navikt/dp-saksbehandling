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
    private companion object {
        private val logger = KotlinLogging.logger {}
    }

    override val jobName: String = "StatistikkJob"

    override suspend fun executeJob() {
        val oppgaveListe: List<Pair<UUID, LocalDateTime>> = statistikkTjeneste.oppgaverTilStatistikk()
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
    }

    override val logger: KLogger = KotlinLogging.logger {}
}
