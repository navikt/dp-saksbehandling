package no.nav.dagpenger.saksbehandling.outbox

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.saksbehandling.job.Job

class OutboxPublisherJob(
    private val publisher: OutboxPublisher,
) : Job() {
    override val jobName: String = "OutboxPublisherJob"
    override val logger: KLogger = KotlinLogging.logger {}

    override suspend fun executeJob() = publisher.publiser()
}
