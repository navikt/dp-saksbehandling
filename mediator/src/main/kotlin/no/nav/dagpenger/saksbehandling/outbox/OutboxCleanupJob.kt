package no.nav.dagpenger.saksbehandling.outbox

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.saksbehandling.job.Job
import java.time.LocalDateTime

class OutboxCleanupJob(
    private val repository: OutboxRepository,
) : Job() {
    override val jobName: String = "OutboxCleanupJob"
    override val logger: KLogger = KotlinLogging.logger {}

    override suspend fun executeJob() {
        val slettet = repository.slettSendteEldreEnn(LocalDateTime.now().minusDays(7))
        logger.info { "Slettet $slettet utgåtte outbox-records (SENDT > 7 dager)" }
    }
}
