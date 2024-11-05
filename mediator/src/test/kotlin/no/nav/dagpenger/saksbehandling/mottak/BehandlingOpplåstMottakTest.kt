package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingOpplåstHendelse
import org.junit.jupiter.api.Test

class BehandlingOpplåstMottakTest {
    private val testRapid = TestRapid()
    private val oppgaveMediatorMock = mockk<OppgaveMediator>(relaxed = true)

    init {
        BehandlingOpplåstMottak(rapidsConnection = testRapid, oppgaveMediator = oppgaveMediatorMock)
    }

    @Test
    fun `Skal håndtere behandling_opplåst hendelse`() {
        testRapid.sendTestMessage(behandlingOpplåstHendelse)

        verify(exactly = 1) {
            oppgaveMediatorMock.settOppgaveUnderBehandling(any<BehandlingOpplåstHendelse>())
        }
    }

    //language=json
    val behandlingOpplåstHendelse =
        """
        {
          "@event_name" : "behandling_endret_tilstand",
          "ident" : "12345612345",
          "behandlingId" : "0192f670-d6d4-7709-927d-d40f25010a5b",
          "forrigeTilstand" : "Låst",
          "gjeldendeTilstand" : "ForslagTilVedtak",
          "forventetFerdig" : "+999999999-12-31T23:59:59.999999999",
          "tidBrukt" : "PT1M22.244965S",
          "@id" : "22bf4304-8c45-46d4-b1e4-f12834c73fcd",
          "@opprettet" : "2024-11-04T10:12:07.082728",
          "system_read_count" : 0,
          "system_participating_services" : [ {
            "id" : "22bf4304-8c45-46d4-b1e4-f12834c73fcd",
            "time" : "2024-11-04T10:12:07.082728"
          } ],
          "@forårsaket_av" : {
            "id" : "658056ec-9aeb-4617-a462-356527eef00f",
            "opprettet" : "2024-11-04T10:12:07.045005",
            "event_name" : "oppgave_returnert_til_saksbehandling"
          }
        }
        """.trimIndent()
}
