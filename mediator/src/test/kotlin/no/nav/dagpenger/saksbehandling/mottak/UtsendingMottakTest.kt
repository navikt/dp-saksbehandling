package no.nav.dagpenger.saksbehandling.mottak

import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.UtsendingMediator
import no.nav.dagpenger.saksbehandling.hendelser.StartUtsendingHendelse
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

internal class UtsendingMottakTest {
    private val testIdent = "12345678901"
    private val behandlingId = UUIDv7.ny()
    private val oppgaveId = UUIDv7.ny()
    private val testRapid = TestRapid()
    private val utsendingMediatorMock = mockk<UtsendingMediator>(relaxed = true)

    init {
        UtsendingMottak(testRapid, utsendingMediatorMock)
    }

    @Test
    fun `Skal behandle vedtak fattet hendelse`() {
        testRapid.sendTestMessage(startUtsending())
        val startUtsendingHendelse =
            StartUtsendingHendelse(
                oppgaveId = oppgaveId,
                behandlingId = behandlingId,
                ident = testIdent,
            )
        verify(exactly = 1) {
            utsendingMediatorMock.mottaStartUtsending(startUtsendingHendelse)
        }
    }

    @Language("JSON")
    private fun startUtsending() =
        """{
          "@event_name": "start_utsending",
          "oppgaveId": "$oppgaveId",
          "behandlingId": "$behandlingId",
          "ident": "$testIdent",
          "@id": "34ae352f-fa37-4552-820f-377dfa671987",
          "@opprettet": "2024-06-06T11:02:15.443504",
          "system_read_count": 0,
          "system_participating_services": [
            {
              "id": "34ae352f-fa37-4552-820f-377dfa671987",
              "time": "2024-06-06T11:02:15.443504"
            }
          ],
          "@forårsaket_av": {
            "id": "0a1ca5e8-e27e-46d3-aa46-6ab7907de324",
            "opprettet": "2024-06-06T11:02:15.155003",
            "event_name": "vedtak_fattet"
          }
        }
        """
}
