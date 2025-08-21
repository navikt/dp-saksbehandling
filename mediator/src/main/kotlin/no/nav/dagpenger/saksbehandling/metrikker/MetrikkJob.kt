package no.nav.dagpenger.saksbehandling.metrikker

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.saksbehandling.db.PostgresDataSourceBuilder.dataSource
import no.nav.dagpenger.saksbehandling.job.Job

class MetrikkJob : Job() {
    override val jobName: String = "MetrikkJob"
    override val logger: KLogger = KotlinLogging.logger {}

    override suspend fun executeJob() {
        logger.info { "Oppdaterer metrikker" }
        oppdaterOppgaveTilstandMetrikker(dataSource)
        oppdaterUtsendingTilstandMetrikker(dataSource)
        oppdaterOppgaveTilstandSiste24TimerMetrikker(dataSource)
    }
}
