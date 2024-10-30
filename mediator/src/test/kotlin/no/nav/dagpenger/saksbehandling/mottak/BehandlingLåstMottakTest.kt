package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.ints.exactly
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
          "@event_name": "behandling_låst",
          "ident": "09830698334",
          "behandlingId": "018ec271-6a29-7fcc-95df-37d48118072f",
          "gjelderDato": "2024-04-09",
          "fagsakId": "0",
          "søknadId": "a830499b-5bcd-4401-9db4-8e54549e9e0f",
          "søknad_uuid": "a830499b-5bcd-4401-9db4-8e54549e9e0f",
          "@id": "7333f08e-dfeb-438e-aba3-9cd6387fca73",
          "@opprettet": "2024-04-10T10:00:21.081950694",
          "system_read_count": 0,
          "system_participating_services": [
            {
              "id": "7333f08e-dfeb-438e-aba3-9cd6387fca73",
              "time": "2024-04-10T10:00:21.081950694",
              "service": "dp-behandling",
              "instance": "dp-behandling-86599cc6d5-lp5kh",
              "image": "europe-north1-docker.pkg.dev/nais-management-233d/teamdagpenger/dp-behandling:2024.04.10-07.51-74cf1b5"
            }
          ]
        }
        """.trimIndent()
}
