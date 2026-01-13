package no.nav.dagpenger.saksbehandling.job

import io.github.oshai.kotlinlogging.KLogger
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.leaderelection.LeaderElector
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Date
import java.util.Timer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.fixedRateTimer
import kotlin.time.Duration.Companion.milliseconds

abstract class Job(
    private val leaderElector: suspend () -> Result<Boolean> = LeaderElector::isLeader,
) {
    private val isRunning = AtomicBoolean(false)

    companion object {
        val now = Date.from(Instant.now().atZone(ZoneId.of("Europe/Oslo")).toInstant())
        val omFemMinutter =
            Date.from(
                Instant
                    .now()
                    .atZone(ZoneId.of("Europe/Oslo"))
                    .toInstant()
                    .plus(Duration.ofMinutes(5)),
            )
        val Int.Dag get() = this * 1000L * 60L * 60L * 24L
        val Int.Minutt get() = this * 1000L * 60L

        fun getNextOccurrence(
            hour: Int,
            minute: Int,
            currentTime: LocalDateTime = LocalDateTime.now(),
            zoneId: ZoneId = ZoneId.systemDefault(),
        ): Date {
            val specifiedTimeToday = currentTime.toLocalDate().atTime(LocalTime.of(hour, minute))

            val nextOccurrence =
                if (currentTime.isBefore(specifiedTimeToday)) {
                    specifiedTimeToday
                } else {
                    specifiedTimeToday.plusDays(1)
                }

            return Date.from(nextOccurrence.atZone(zoneId).toInstant())
        }
    }

    abstract val jobName: String

    abstract suspend fun executeJob()

    protected abstract val logger: KLogger

    fun startJob(
        daemon: Boolean = true,
        startAt: Date = omFemMinutter,
        period: Long = 1.Dag,
    ): Timer {
        logger.info { "Jobb $jobName vil kjøre med intervall ${period.milliseconds.inWholeMinutes} minutter med første kjøring $startAt" }
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
            // Prevent overlapping executions on the same pod
            if (!isRunning.compareAndSet(false, true)) {
                logger.warn { "Jobb $jobName kjører fortsatt fra forrige intervall, hopper over denne kjøringen" }
                return@runBlocking
            }

            try {
                leaderElector()
                    .onSuccess {
                        when (it) {
                            true -> {
                                logger.info { "Starter jobb $jobName" }
                                executeJob()
                            }

                            false -> logger.info { "Er ikke leder, kjører ikke jobb: $jobName" }
                        }
                    }.onFailure {
                        logger.error(it) { "Kunne ikke avgjøre om jeg er leder for jobb: $jobName" }
                    }
            } finally {
                isRunning.set(false)
            }
        }
    }
}
