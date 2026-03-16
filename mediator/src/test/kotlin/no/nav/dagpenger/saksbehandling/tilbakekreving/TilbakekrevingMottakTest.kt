package no.nav.dagpenger.saksbehandling.tilbakekreving

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.hendelser.TilbakekrevingHendelse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.UUID

class TilbakekrevingMottakTest {
    private val testRapid = TestRapid()
    private val oppgaveMediator = mockk<OppgaveMediator>()
    private val ident = "12345678901"
    private val tilbakekrevingBehandlingId = UUID.randomUUID()

    init {
        every { oppgaveMediator.håndter(any<TilbakekrevingHendelse>()) } just Runs
        TilbakekrevingMottak(
            rapidsConnection = testRapid,
            oppgaveMediator = oppgaveMediator,
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["OPPRETTET", "TIL_BEHANDLING", "TIL_GODKJENNING", "AVSLUTTET"])
    fun `Skal motta hendelse for alle statuser og kalle oppgaveMediator`(status: String) {
        testRapid.sendTestMessage(tilbakekrevingMelding(status), ident)
        verify(exactly = 1) { oppgaveMediator.håndter(any<TilbakekrevingHendelse>()) }
    }

    @Test
    fun `Skal kaste feil for meldinger uten key`() {
        assertThrows<IllegalArgumentException> {
            testRapid.sendTestMessage(tilbakekrevingMelding("OPPRETTET"))
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
        verify(exactly = 0) { oppgaveMediator.håndter(any<TilbakekrevingHendelse>()) }
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
            "sakOpprettet": "2024-05-20T08:00:00",
            "varselSendt": "2024-05-21",
            "behandlingsstatus": "$status",
            "forrigeBehandlingsstatus": null,
            "totaltFeilutbetaltBeløp": "15000",
            "saksbehandlingURL": "https://tilbakekreving.intern.nav.no/behandling/$tilbakekrevingBehandlingId",
            "fullstendigPeriode": {
              "fom": "2025-01-01",
              "tom": "2025-06-30"
            }
          }
        }
        """.trimIndent()
}
