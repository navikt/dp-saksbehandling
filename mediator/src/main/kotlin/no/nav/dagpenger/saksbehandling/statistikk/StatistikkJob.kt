package no.nav.dagpenger.saksbehandling.statistikk

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.TransactionalSession
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.job.Job
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

class StatistikkJob(
    private val rapidsConnection: RapidsConnection,
    private val oppgaveMediator: OppgaveMediator,
    private val sakMediator: SakMediator,
    private val statistikkTjeneste: StatistikkTjeneste,
    private val session: DataSource,
) : Job() {
    private companion object {
        private val logger = KotlinLogging.logger {}
    }

    override val jobName: String = "StatistikkJob"

    override suspend fun executeJob() {
        val list = statistikkTjeneste.hentOppgaver(LocalDateTime.now())
        rapidsConnection.publish("sadf") //denne tryner
        statistikkTjeneste.oppdaterOppgaver( list)

    }
    override val logger: KLogger = KotlinLogging.logger {}
}


