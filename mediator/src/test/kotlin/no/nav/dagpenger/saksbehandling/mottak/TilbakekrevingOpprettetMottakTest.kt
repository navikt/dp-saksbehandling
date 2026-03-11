package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.hendelser.TilbakekrevingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import org.junit.jupiter.api.Test
import java.util.UUID

class TilbakekrevingOpprettetMottakTest {
    private val testRapid = TestRapid()
    private val oppgaveMediatorMock = mockk<OppgaveMediator>(relaxed = true)
    private val sakMediatorMock = mockk<SakMediator>(relaxed = true)
    private val ident = "12345678901"
    private val tilbakekrevingBehandlingId = UUID.randomUUID()

    init {
        TilbakekrevingOpprettetMottak(
            rapidsConnection = testRapid,
            oppgaveMediator = oppgaveMediatorMock,
            sakMediator = sakMediatorMock,
        )
    }

    @Test
    fun `Skal opprette oppgave for tilbakekreving med status OPPRETTET`() {
        testRapid.sendTestMessage(tilbakekrevingOpprettetMelding(), ident)

        verify(exactly = 1) {
            oppgaveMediatorMock.opprettOppgaveForTilbakekreving(
                match<TilbakekrevingOpprettetHendelse> {
                    it.ident == ident &&
                        it.eksternFagsakId == "100001234" &&
                        it.tilbakekrevingBehandlingId == tilbakekrevingBehandlingId &&
                        it.totaltFeilutbetaltBeløp.compareTo(java.math.BigDecimal("15000")) == 0
                },
            )
        }
    }

    @Test
    fun `Skal ignorere tilbakekreving med annen status enn OPPRETTET`() {
        testRapid.sendTestMessage(tilbakekrevingMeldingMedStatus("TIL_BEHANDLING"), ident)

        verify(exactly = 0) {
            oppgaveMediatorMock.opprettOppgaveForTilbakekreving(any())
        }
    }

    @Test
    fun `Skal ignorere meldinger uten key`() {
        testRapid.sendTestMessage(tilbakekrevingOpprettetMelding())

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
    private fun tilbakekrevingOpprettetMelding() =
        """
        {
          "hendelsestype": "behandling_endret",
          "versjon": 1,
          "eksternFagsakId": "100001234",
          "eksternBehandlingId": "dp-behandling-id-123",
          "hendelseOpprettet": "2024-06-01T10:00:00",
          "tilbakekreving": {
            "behandlingId": "$tilbakekrevingBehandlingId",
            "behandlingsstatus": "OPPRETTET",
            "totaltFeilutbetaltBeløp": "15000"
          }
        }
        """.trimIndent()

    //language=json
    private fun tilbakekrevingMeldingMedStatus(status: String) =
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
