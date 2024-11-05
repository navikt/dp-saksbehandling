package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingLåstHendelse
import org.junit.jupiter.api.Test

class BehandlingLåstMottakTest {
    private val testRapid = TestRapid()
    private val oppgaveMediatorMock = mockk<OppgaveMediator>(relaxed = true)

    init {
        BehandlingLåstMottak(rapidsConnection = testRapid, oppgaveMediator = oppgaveMediatorMock)
    }

    @Test
    fun `Skal håndtere behandling_låst hendelse`() {
        testRapid.sendTestMessage(behandlingLåstHendelse)

        verify(exactly = 1) {
            oppgaveMediatorMock.settOppgaveKlarTilKontroll(any<BehandlingLåstHendelse>())
        }
    }

    //language=json
    val behandlingLåstHendelse =
        """
        {
          "@event_name" : "behandling_endret_tilstand",
          "ident" : "12345612345",
          "behandlingId" : "0192f670-d6d4-7709-927d-d40f25010a5b",
          "forrigeTilstand" : "ForslagTilVedtak",
          "gjeldendeTilstand" : "Låst",
          "forventetFerdig" : "+999999999-12-31T23:59:59.999999999",
          "tidBrukt" : "PT0.145965S",
          "@id" : "95318c13-94c3-4d78-b0c0-46421a265552",
          "@opprettet" : "2024-11-04T10:10:44.837634",
          "system_read_count" : 0,
          "system_participating_services" : [ {
            "id" : "95318c13-94c3-4d78-b0c0-46421a265552",
            "time" : "2024-11-04T10:10:44.837634"
          } ],
          "@forårsaket_av" : {
            "id" : "9f1276b3-0ff9-4c4f-9e68-b6d9c644863f",
            "opprettet" : "2024-11-04T10:10:44.80971",
            "event_name" : "oppgave_sendt_til_kontroll"
          }
        }
        """.trimIndent()
}
