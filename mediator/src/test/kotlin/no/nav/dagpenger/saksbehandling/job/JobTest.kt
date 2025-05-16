package no.nav.dagpenger.saksbehandling.job

import io.kotest.assertions.nondeterministic.continually
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import mu.KLogger
import mu.KotlinLogging
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
                now.toLocalDate().atTime(14, 0).atZone(ZoneId.systemDefault()).toInstant(),
            )

        Job.getNextOccurrence(12, 0, now) shouldBe
            Date.from(
                now.toLocalDate().plusDays(1).atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant(),
            )
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
}
