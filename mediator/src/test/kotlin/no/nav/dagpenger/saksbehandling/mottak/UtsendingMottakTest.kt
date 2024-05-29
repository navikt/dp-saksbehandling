package no.nav.dagpenger.saksbehandling.mottak

import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.UtsendingMediator
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

internal class UtsendingMottakTest {
    val oppgaveId = UUIDv7.ny()

    private val testRapid = TestRapid()
    private val utsendingMediatorMock = mockk<UtsendingMediator>(relaxed = true)

    init {
        UtsendingMottak(testRapid, utsendingMediatorMock)
    }

    @Test
    fun `Skal behandle vedtak fattet hendelse`() {
        testRapid.sendTestMessage(startUtsendingHendelse(oppgaveId.toString()))
        verify(exactly = 1) {
            utsendingMediatorMock.startUtsending(oppgaveId)
        }
    }

    @Language("JSON")
    private fun startUtsendingHendelse(oppgaveId: String) =
        """
        {
            "@event_name": "start_utsending",
            "oppgaveId": "$oppgaveId"
        }
        """
}
