package no.nav.dagpenger.saksbehandling.oppgave

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.saksbehandling.AlertManager
import no.nav.dagpenger.saksbehandling.HendelseBehandler
import no.nav.dagpenger.saksbehandling.UUIDv7
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class OppgaveTilstandAlertJobTest {
    private val testRapid = TestRapid()

    private val oppgaver =
        listOf(
            AlertManager.OppgaveOpprettetTilstandAlert(
                oppgaveId = UUIDv7.ny(),
                sistEndret = LocalDateTime.now(),
                utløstAvType = HendelseBehandler.valueOf("SØKNAD"),
            ),
            AlertManager.OppgaveOpprettetTilstandAlert(
                oppgaveId = UUIDv7.ny(),
                sistEndret = LocalDateTime.now().minusMinutes(5),
                utløstAvType = HendelseBehandler.valueOf("MELDEKORT"),
            ),
        )

    @Test
    fun sendUtAlert() {
        runBlocking {
            OppgaveTilstandAlertJob(
                rapidsConnection = testRapid,
                oppgaveTilAlertRepository =
                    mockk<OppgaveTilAlertRepository>().also {
                        every { it.hentOppgaverSomSkalVarsles() } returns oppgaver
                    },
            ).executeJob()
        }

        testRapid.inspektør.size shouldBe 2

        testRapid.inspektør.message(0).let { jsonNode ->
            jsonNode["alertType"].stringValue() shouldBe "OPPGAVE_OPPRETTET_TILSTAND_ALERT"
            jsonNode["@event_name"].stringValue() shouldBe "saksbehandling_alert"
        }
    }
}
