package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingTilGodkjenningHendelse
import org.junit.jupiter.api.Test
import java.util.UUID

class BehandlingTilGodkjenningMottakTest {
    private val testRapid = TestRapid()
    private val oppgaveMediatorMock = mockk<OppgaveMediator>(relaxed = true)
    private val ident = "09830698334"
    private val behandlingId = UUID.fromString("018ec271-6a29-7fcc-95df-37d48118072f")

    init {
        BehandlingTilGodkjenningMottak(rapidsConnection = testRapid, oppgaveMediator = oppgaveMediatorMock)
    }

    @Test
    fun `Skal kalle behandlingTilGodkjenning når gjeldendeTilstand er TilGodkjenning`() {
        testRapid.sendTestMessage(behandlingEndretTilstandEvent(gjeldendeTilstand = "TilGodkjenning"))

        verify(exactly = 1) {
            oppgaveMediatorMock.behandlingTilGodkjenning(
                BehandlingTilGodkjenningHendelse(
                    behandlingId = behandlingId,
                    ident = ident,
                ),
            )
        }
    }

    @Test
    fun `Skal ignorere hendelse når gjeldendeTilstand ikke er TilGodkjenning`() {
        testRapid.sendTestMessage(behandlingEndretTilstandEvent(gjeldendeTilstand = "TilBeslutning"))
        testRapid.sendTestMessage(behandlingEndretTilstandEvent(gjeldendeTilstand = "UnderBehandling"))
        testRapid.sendTestMessage(behandlingEndretTilstandEvent(gjeldendeTilstand = "Ferdig"))

        verify(exactly = 0) {
            oppgaveMediatorMock.behandlingTilGodkjenning(any())
        }
    }

    @Test
    fun `Skal ignorere andre event_name`() {
        testRapid.sendTestMessage(
            """
            {
              "@event_name": "behandling_opprettet",
              "gjeldendeTilstand": "TilGodkjenning",
              "ident": "$ident",
              "behandlingId": "$behandlingId"
            }
            """.trimIndent(),
        )

        verify(exactly = 0) {
            oppgaveMediatorMock.behandlingTilGodkjenning(any())
        }
    }

    //language=json
    private fun behandlingEndretTilstandEvent(gjeldendeTilstand: String) =
        """
        {
          "@event_name": "behandling_endret_tilstand",
          "ident": "$ident",
          "behandlingId": "$behandlingId",
          "forrigeTilstand": "TilBeslutning",
          "gjeldendeTilstand": "$gjeldendeTilstand",
          "@id": "7333f08e-dfeb-438e-aba3-9cd6387fca73",
          "@opprettet": "2024-04-10T10:00:21.081950694"
        }
        """.trimIndent()
}
