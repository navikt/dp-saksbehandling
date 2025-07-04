package no.nav.dagpenger.saksbehandling.utsending.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.UtsendingSak
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator
import no.nav.dagpenger.saksbehandling.utsending.hendelser.StartUtsendingHendelse
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

internal class UtsendingMottakTest {
    private val testIdent = "12345678901"
    private val behandlingId = UUIDv7.ny()
    private val oppgaveId = UUIDv7.ny()
    private val utsendingSak =
        UtsendingSak(
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
                utsendingSak = utsendingSak,
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
              "id": "${utsendingSak.id}",
              "kontekst": "${utsendingSak.kontekst}"
          }
        }
        """
}
