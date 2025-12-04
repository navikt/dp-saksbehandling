package no.nav.dagpenger.saksbehandling.oppgave

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.TestHelper.lagOppgave
import org.junit.jupiter.api.Test

class OppgaveTilstandAlertJobTest {
    private val testRapid = TestRapid()
    val oppgave = lagOppgave(tilstand = Oppgave.Opprettet)

    @Test
    fun sendUtAlert() {
        runBlocking {
            OppgaveTilstandAlertJob(
                rapidsConnection = testRapid,
                oppgaveMediator =
                    mockk<OppgaveMediator>().also {
                        every { it.hentAlleOppgaverMedTilstand(Oppgave.Tilstand.Type.OPPRETTET) } returns
                            listOf(
                                oppgave, oppgave,
                            )
                    },
            ).executeJob()
        }

        testRapid.inspektør.size shouldBe 2

        testRapid.inspektør.message(0).let { jsonNode ->
            jsonNode["alertType"].asText() shouldBe "OPPGAVE_OPPRETTET_TILSTAND_ALERT"
            jsonNode["@event_name"].asText() shouldBe "saksbehandling_alert"
        }
    }
}
