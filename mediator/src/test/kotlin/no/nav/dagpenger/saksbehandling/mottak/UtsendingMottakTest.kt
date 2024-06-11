package no.nav.dagpenger.saksbehandling.mottak

import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.Sak
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
    private val sak =
        Sak(
            id = "sakId",
            kontekst = "fagsystem",
        )
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
                sak = sak,
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
          "sak": {
              "id": "${sak.id}",
              "kontekst": "${sak.kontekst}"
          }
        }
        """
}
