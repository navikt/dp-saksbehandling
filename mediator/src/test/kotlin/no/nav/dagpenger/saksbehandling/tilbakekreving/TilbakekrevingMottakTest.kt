package no.nav.dagpenger.saksbehandling.tilbakekreving

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.UUID

class TilbakekrevingMottakTest {
    private val testRapid = TestRapid()
    private val oppgaveMediatorMock = mockk<OppgaveMediator>(relaxed = true)
    private val ident = "12345678901"
    private val tilbakekrevingBehandlingId = UUID.randomUUID()

    init {
        TilbakekrevingMottak(
            rapidsConnection = testRapid,
            oppgaveMediator = oppgaveMediatorMock,
        )
    }

    @Test
    fun `Skal opprette oppgave for tilbakekreving med status OPPRETTET`() {
        testRapid.sendTestMessage(tilbakekrevingMelding("OPPRETTET"), ident)

        verify(exactly = 1) {
            oppgaveMediatorMock.opprettOppgaveForTilbakekreving(
                match<TilbakekrevingHendelse> {
                    it.ident == ident &&
                        it.eksternFagsakId == "100001234" &&
                        it.tilbakekrevingBehandlingId == tilbakekrevingBehandlingId &&
                        it.totaltFeilutbetaltBeløp.compareTo(java.math.BigDecimal("15000")) == 0 &&
                        it.status == TilbakekrevingHendelse.BehandlingStatus.OPPRETTET
                },
            )
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["TIL_BEHANDLING", "TIL_GODKJENNING", "AVSLUTTET"])
    fun `Skal motta hendelse for alle statuser uten å opprette oppgave`(status: String) {
        testRapid.sendTestMessage(tilbakekrevingMelding(status), ident)

        verify(exactly = 0) {
            oppgaveMediatorMock.opprettOppgaveForTilbakekreving(any())
        }
    }

    @Test
    fun `Skal ignorere meldinger uten key`() {
        testRapid.sendTestMessage(tilbakekrevingMelding("OPPRETTET"))

        verify(exactly = 0) {
            oppgaveMediatorMock.opprettOppgaveForTilbakekreving(any())
        }
    }

    @Test
    fun `Skal ignorere meldinger som ikke er behandling_endret`() {
        testRapid.sendTestMessage(
            //language=json
            """
            {
              "hendelsestype": "noe_annet",
              "tilbakekreving": {
                "behandlingsstatus": "OPPRETTET",
                "behandlingId": "$tilbakekrevingBehandlingId",
                "totaltFeilutbetaltBeløp": "15000"
              },
              "eksternFagsakId": "100001234",
              "hendelseOpprettet": "2024-06-01T10:00:00"
            }
            """.trimIndent(),
            ident,
        )

        verify(exactly = 0) {
            oppgaveMediatorMock.opprettOppgaveForTilbakekreving(any())
        }
    }

    //language=json
    private fun tilbakekrevingMelding(status: String) =
        """
        {
          "hendelsestype": "behandling_endret",
          "versjon": 1,
          "eksternFagsakId": "100001234",
          "eksternBehandlingId": "dp-behandling-id-123",
          "hendelseOpprettet": "2024-06-01T10:00:00",
          "tilbakekreving": {
            "behandlingId": "$tilbakekrevingBehandlingId",
            "behandlingsstatus": "$status",
            "totaltFeilutbetaltBeløp": "15000"
          }
        }
        """.trimIndent()
}
