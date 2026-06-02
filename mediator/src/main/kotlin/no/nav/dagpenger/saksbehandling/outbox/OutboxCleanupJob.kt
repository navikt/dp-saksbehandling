package no.nav.dagpenger.saksbehandling.outbox

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.saksbehandling.job.Job

class OutboxCleanupJob(
    private val outbox: OutboxVedlikehold,
) : Job() {
    override val jobName: String = "OutboxCleanupJob"
    override val logger: KLogger = KotlinLogging.logger {}

    override suspend fun executeJob() {
        outbox.slettGamleSendte()
    }
}
