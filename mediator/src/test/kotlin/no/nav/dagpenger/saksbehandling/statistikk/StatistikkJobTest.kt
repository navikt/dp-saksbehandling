package no.nav.dagpenger.saksbehandling.statistikk

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.assertions.json.shouldEqualJson
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.TestHelper
import no.nav.dagpenger.saksbehandling.TestHelper.lagTilstandLogg
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class StatistikkJobTest {
    private val testRapid = TestRapid()

    private val sakMediator =
        mockk<SakMediator>(relaxed = true).also {
            every { it.hentSakIdForBehandlingId(any()) } returns UUID.randomUUID()
        }

    val now = LocalDateTime.now()

    private val statistikkTjeneste =
        mockk<StatistikkTjeneste>().also {
            every { it.hentOppgaver() } returns
                listOf(
                    Pair(UUID.randomUUID(), now),
                    Pair(UUID.randomUUID(), now),
                )
            every { it.oppdaterOppgaver(any()) } returns 2
        }
    private val oppgaveRepository =
        mockk<OppgaveRepository>().also {
            every {
                it.hentOppgave(any())
            } returns TestHelper.lagOppgave(oppgaveId = UUID.randomUUID(), tilstandslogg = lagTilstandLogg())
        }

    @Test
    fun `dummy test`() {
        runBlocking {
            StatistikkJob(
                rapidsConnection = testRapid,
                sakMediator = sakMediator,
                statistikkTjeneste = statistikkTjeneste,
                oppgaveRepository = oppgaveRepository,
            ).executeJob()
        }

        testRapid.inspektør.message(0).toString() shouldEqualJson
            """
            {
            
            }
            """.trimIndent()
    }
}
