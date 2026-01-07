package no.nav.dagpenger.saksbehandling.statistikk

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
import no.nav.dagpenger.saksbehandling.job.Job
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import no.nav.dagpenger.saksbehandling.statistikk.toJson

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
        val list = statistikkTjeneste.hentOppgaver()
        val oppgaver =
            list.map {
                val oppgave = oppgaveRepository.hentOppgave(it.first)
                val sakId = sakMediator.hentSakIdForBehandlingId(oppgave.behandling.behandlingId)
                StatistikkOppgave(
                    oppgave = oppgave,
                    sakId = sakId,
                )
            }

        rapidsConnection.publish(
            //language=JSON
            """
            {
              "@event_name": "statistikk_job",
              "statistikkOppgaver":  ${oppgaver.toJson()}
              
            }  
            """.trimIndent(),
        )
    }

    override val logger: KLogger = KotlinLogging.logger {}
}

private fun List<StatistikkOppgave>.toJson(): String =
    this.joinToString(
        prefix = "[",
        postfix = "]",
        separator = ",",
    ) { oppgave -> oppgave.toJson() }
