package no.nav.dagpenger.saksbehandling.job

import io.kotest.assertions.nondeterministic.continually
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import mu.KLogger
import mu.KotlinLogging
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.seconds

class JobTest {
    @Test
    fun `Skal trigge job hvis leder`() {
        runBlocking {
            val testJob = TestJob()
            testJob.startJob(period = 500L)

            eventually(1.seconds) {
                testJob.antallGangerKjørt shouldBeGreaterThan 1
            }
        }
    }

    @Test
    fun `Skal ikke trigge job hvis ikke leder`() {
        runBlocking {
            val testJob = TestJob(leaderElector = { Result.success(false) })
            testJob.startJob(period = 50L)

            continually(1.seconds) {
                testJob.antallGangerKjørt shouldBe 0
            }
        }
    }

    @Test
    fun `Skal ikke trigge job dersom leader election feiler`() {
        runBlocking {
            val testJob = TestJob(leaderElector = { Result.failure(RuntimeException("Feil")) })
            testJob.startJob(period = 50L)

            continually(1.seconds) {
                testJob.antallGangerKjørt shouldBe 0
            }
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
}
