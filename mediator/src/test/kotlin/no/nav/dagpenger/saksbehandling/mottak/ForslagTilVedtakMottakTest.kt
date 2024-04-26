package no.nav.dagpenger.saksbehandling.mottak

import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test

class ForslagTilVedtakMottakTest {
    private val testRapid = TestRapid()
    private val oppgaveMediator = mockk<OppgaveMediator>(relaxed = true)

    init {
        ForslagTilVedtakMottak(testRapid, oppgaveMediator)
    }

    @Test
    fun `Skal kunne motta forslag til vedtak events`() {
        testRapid.sendTestMessage(forslagTilVedtakJson)
        verify(exactly = 1) {
            oppgaveMediator.settOppgaveKlarTilBehandling(any<ForslagTilVedtakHendelse>())
        }
    }

    //language=json
    val forslagTilVedtakJson =
        """
        {
          "@event_name" : "forslag_til_vedtak",
          "ident" : "11109233444",
          "behandlingId" : "018e0ed1-f6ea-7257-a8d0-e5acb533fcb8",
          "søknadId" : "4afce924-6cb4-4ab4-a92b-fe91e24f31bf",
          "@id" : "81080541-8755-488b-ac1d-94169169e2a3",
          "@opprettet" : "2024-03-05T14:33:45.191117",
          "system_read_count" : 0,
          "system_participating_services" : [ {
            "id" : "81080541-8755-488b-ac1d-94169169e2a3",
            "time" : "2024-03-05T14:33:45.191117"
          } ]
        }
        """.trimIndent()
}
