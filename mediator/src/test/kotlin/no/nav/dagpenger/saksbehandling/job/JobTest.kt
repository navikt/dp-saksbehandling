package no.nav.dagpenger.saksbehandling.job

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.kotest.assertions.nondeterministic.continually
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.prometheus.metrics.model.registry.PrometheusRegistry
import io.prometheus.metrics.model.snapshots.CounterSnapshot
import io.prometheus.metrics.model.snapshots.GaugeSnapshot
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.getSnapShot
import no.nav.dagpenger.saksbehandling.job.Job.Companion.now
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import kotlin.time.Duration.Companion.seconds

class JobTest {
    @Test
    fun `Skal trigge job hvis leder`() {
        runBlocking {
            val testJob = TestJob()
            testJob.startJob(startAt = now, period = 500L)

            eventually(1.seconds) {
                testJob.antallGangerKjørt shouldBeGreaterThan 1
            }
        }
    }

    @Test
    fun `Skal ikke trigge job hvis ikke leder`() {
        runBlocking {
            val testJob = TestJob(leaderElector = { Result.success(false) })
            testJob.startJob(startAt = now, period = 50L)

            continually(1.seconds) {
                testJob.antallGangerKjørt shouldBe 0
            }
        }
    }

    @Test
    fun `Skal ikke trigge job dersom leader election feiler`() {
        runBlocking {
            val testJob = TestJob(leaderElector = { Result.failure(RuntimeException("Feil")) })
            testJob.startJob(startAt = now, period = 50L)

            continually(1.seconds) {
                testJob.antallGangerKjørt shouldBe 0
            }
        }
    }

    @Test
    fun `Skal finne riktig neste tidspunkt`() {
        val now = LocalDateTime.of(2023, 10, 10, 13, 0)

        Job.getNextOccurrence(14, 0, now) shouldBe
            Date.from(
                now
                    .toLocalDate()
                    .atTime(14, 0)
                    .atZone(ZoneId.systemDefault())
                    .toInstant(),
            )

        Job.getNextOccurrence(12, 0, now) shouldBe
            Date.from(
                now
                    .toLocalDate()
                    .plusDays(1)
                    .atTime(12, 0)
                    .atZone(ZoneId.systemDefault())
                    .toInstant(),
            )
    }

    @Test
    fun `Skal fortsette aa kjore selv om jobben kaster exception`() {
        runBlocking {
            val testJob = KastendeTestJob()
            testJob.startJob(startAt = now, period = 200L)

            // Hvis Timer-tråden hadde dødd ved første kast ville antallForsøk stoppet på 1
            eventually(3.seconds) {
                testJob.antallForsøk shouldBeGreaterThan 2
            }

            val failures =
                PrometheusRegistry.defaultRegistry
                    .getSnapShot<CounterSnapshot> { it == "dp_saksbehandling_job_executions_total" }
                    .dataPoints
                    .single { it.labels["job"] == "kastendeTestJob" && it.labels["status"] == "failure" }
                    .value
            failures shouldBeGreaterThan 2.0
        }
    }

    @Test
    fun `Skal seede last_success ved jobbstart slik at staleness-alarmen har en serie aa joine paa`() {
        runBlocking {
            val testJob = KastendeTestJob()
            testJob.startJob(startAt = now, period = 200L)

            // Selv om jobben aldri lykkes, må last_success-serien finnes (seedet ved start).
            // Uten seed ville serien mangle og staleness-alarmen (join på `job`) aldri fyre.
            val lastSuccess =
                PrometheusRegistry.defaultRegistry
                    .getSnapShot<GaugeSnapshot> { it == "dp_saksbehandling_job_last_success_timestamp_seconds" }
                    .dataPoints
                    .single { it.labels["job"] == "kastendeTestJob" }
                    .value
            lastSuccess shouldBeGreaterThan 0.0
        }
    }

    private class TestJob(
        leaderElector: suspend () -> Result<Boolean> = { Result.success(true) },
    ) : Job(leaderElector) {
        var antallGangerKjørt: Int = 0
        override val jobName: String = "testJob"

        override suspend fun executeJob() {
            antallGangerKjørt++
        }

        override val logger: KLogger = KotlinLogging.logger {}
    }

    private class KastendeTestJob(
        leaderElector: suspend () -> Result<Boolean> = { Result.success(true) },
    ) : Job(leaderElector) {
        var antallForsøk: Int = 0
        override val jobName: String = "kastendeTestJob"

        override suspend fun executeJob() {
            antallForsøk++
            throw RuntimeException("Simulert jobb-feil")
        }

        override val logger: KLogger = KotlinLogging.logger {}
    }
}
