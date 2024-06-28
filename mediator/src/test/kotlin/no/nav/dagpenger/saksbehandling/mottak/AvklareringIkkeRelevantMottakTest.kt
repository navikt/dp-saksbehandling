package no.nav.dagpenger.saksbehandling.mottak

import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.hendelser.IkkeRelevantAvklaringHendelse
import no.nav.helse.rapids_rivers.testsupport.TestRapid
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
        testRapid.sendTestMessage(avklaringIkkeRelevantMelding())

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

    private fun avklaringIkkeRelevantMelding(): String {
        return """
                {
                "@event_name": "AvklaringIkkeRelevant",
                "ident": "$testIdent",
                "behandlingId": "$behandlingId",
                "avklaringId": "123e4567-e89b-12d3-a456-426614174000",
                "kode": "$ikkeRelevantEmneknagg"
            }
            """.trimIndent()
    }
}
