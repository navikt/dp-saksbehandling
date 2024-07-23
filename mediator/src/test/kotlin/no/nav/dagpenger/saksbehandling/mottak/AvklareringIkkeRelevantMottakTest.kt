package no.nav.dagpenger.saksbehandling.mottak

import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.hendelser.IkkeRelevantAvklaringHendelse
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class AvklareringIkkeRelevantMottakTest {
    private val testRapid = TestRapid()
    private val testIdent = "12345678910"
    private val behandlingId = UUIDv7.ny()
    private val ikkeRelevantEmneknagg = "AVKLARING_KODE"

    @Test
    fun `Skal håndtere riktig hendelse`() {
        val oppgaveMediatorMock = mockk<OppgaveMediator>(relaxed = true)

        AvklareringIkkeRelevantMottak(testRapid, oppgaveMediatorMock)
        testRapid.sendTestMessage(avklaringLukketHendelse)

        verify(exactly = 1) {
            oppgaveMediatorMock.fjernEmneknagg(
                IkkeRelevantAvklaringHendelse(
                    ident = testIdent,
                    behandlingId = behandlingId,
                    ikkeRelevantEmneknagg = ikkeRelevantEmneknagg,
                ),
            )
        }
    }

    @Language("JSON")
    private val avklaringLukketHendelse =
        """
        {
          "@event_name": "avklaring_lukket",
          "avklaringId": "0190baa0-f88f-73b0-8921-ad6865f3c6eb",
          "ident": "$testIdent",
          "kode": "$ikkeRelevantEmneknagg",
          "behandlingId": "$behandlingId"
        }
        """.trimIndent()
}
