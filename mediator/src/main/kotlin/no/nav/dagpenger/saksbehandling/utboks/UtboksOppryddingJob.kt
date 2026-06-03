package no.nav.dagpenger.saksbehandling.utboks

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.saksbehandling.job.Job

class UtboksOppryddingJob(
    private val utboks: UtboksVedlikehold,
) : Job() {
    override val jobName: String = "UtboksOppryddingJob"
    override val logger: KLogger = KotlinLogging.logger {}

    override suspend fun executeJob() {
        utboks.slettGamleSendte()
    }
}
