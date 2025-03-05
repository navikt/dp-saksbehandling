package no.nav.dagpenger.saksbehandling.job

import kotlinx.coroutines.runBlocking
import mu.KLogger
import no.nav.dagpenger.saksbehandling.leaderelection.LeaderElector
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.Timer
import kotlin.concurrent.fixedRateTimer

abstract class Job(
    private val leaderElector: suspend () -> Result<Boolean> = LeaderElector::isLeader,
) {
    companion object {
        val now = Date.from(Instant.now().atZone(ZoneId.of("Europe/Oslo")).toInstant())
        val omFemMinutter =
            Date.from(Instant.now().atZone(ZoneId.of("Europe/Oslo")).toInstant().plus(Duration.ofMinutes(10)))
        val Int.Dag get() = this * 1000L * 60L * 60L * 24L
        val Int.Minutt get() = this * 1000L * 60L
    }

    abstract val jobName: String

    abstract suspend fun executeJob()

    protected abstract val logger: KLogger

    fun startJob(
        daemon: Boolean = true,
        startAt: Date = omFemMinutter,
        period: Long = 1.Dag,
    ): Timer {
        return fixedRateTimer(
            name = jobName,
            daemon = daemon,
            startAt = startAt,
            period = period,
            action = { executeJobIfLeader() },
        )
    }

    private fun executeJobIfLeader() {
        runBlocking {
            leaderElector()
                .onSuccess {
                    when (it) {
                        true -> {
                            logger.info { "Starter jobb $jobName" }
                            executeJob()
                        }

                        false -> logger.info { "Er ikke leder, kjører ikke jobb: $jobName" }
                    }
                }
                .onFailure {
                    logger.error(it) { "Kunne ikke avgjøre om jeg er leder for jobb: $jobName" }
                }
        }
    }
}
