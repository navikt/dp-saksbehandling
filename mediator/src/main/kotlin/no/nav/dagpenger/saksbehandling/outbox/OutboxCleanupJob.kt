package no.nav.dagpenger.saksbehandling.outbox

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.job.Job
import java.time.LocalDateTime
import javax.sql.DataSource

class OutboxCleanupJob(
    private val dataSource: DataSource,
) : Job() {
    override val jobName: String = "OutboxCleanupJob"
    override val logger: KLogger = KotlinLogging.logger {}

    override suspend fun executeJob() {
        val slettet =
            sessionOf(dataSource).use { session ->
                session.run(
                    queryOf(
                        //language=PostgreSQL
                        statement = "DELETE FROM outbox WHERE status = 'SENDT' AND created_at < :cutoff",
                        paramMap = mapOf("cutoff" to LocalDateTime.now().minusDays(7)),
                    ).asUpdate,
                )
            }
        logger.info { "Slettet $slettet utgåtte outbox-records (SENDT > 7 dager)" }
    }
}
