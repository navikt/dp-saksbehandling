package no.nav.dagpenger.saksbehandling.utboks

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.saksbehandling.job.Job

class UtboksPubliseringJob(
    private val vedlikehold: UtboksVedlikehold,
) : Job() {
    override val jobName: String = "UtboksPubliseringJob"
    override val logger: KLogger = KotlinLogging.logger {}

    override suspend fun executeJob() = vedlikehold.publiserVentende()
}
