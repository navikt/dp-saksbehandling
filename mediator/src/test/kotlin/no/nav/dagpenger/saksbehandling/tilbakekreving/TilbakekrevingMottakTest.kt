package no.nav.dagpenger.saksbehandling.tilbakekreving

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.hendelser.TilbakekrevingHendelse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class TilbakekrevingMottakTest {
    private val testRapid = TestRapid()
    private val oppgaveMediator = mockk<OppgaveMediator>()
    private val ident = "12345678901"
    private val tilbakekrevingBehandlingId = UUID.randomUUID()
    private val behandlingId = UUID.randomUUID()

    init {
        every { oppgaveMediator.håndter(any<TilbakekrevingHendelse>()) } just Runs
        TilbakekrevingMottak(
            rapidsConnection = testRapid,
            oppgaveMediator = oppgaveMediator,
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["OPPRETTET", "TIL_FORHÅNDSVARSEL", "TIL_BEHANDLING", "TIL_GODKJENNING", "AVSLUTTET"])
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
    fun `Skal parse tilbakekrevingHendelse korrekt`() {
        val slot = slot<TilbakekrevingHendelse>()
        testRapid.sendTestMessage(tilbakekrevingMelding("OPPRETTET"), ident)
        verify(exactly = 1) { oppgaveMediator.håndter(capture(slot)) }
        val hendelse = slot.captured
        hendelse.eksternFagsakId shouldBe "100001234"
        hendelse.eksternBehandlingId shouldBe behandlingId
        hendelse.ident shouldBe ident
        hendelse.hendelseOpprettet shouldBe LocalDateTime.parse("2024-06-01T10:00:00")
        hendelse.tilbakekreving shouldBe
            TilbakekrevingHendelse.Tilbakekreving(
                behandlingId = tilbakekrevingBehandlingId,
                opprettet = LocalDateTime.parse("2024-05-20T08:00:00"),
                varselSendt = LocalDate.parse("2024-05-21"),
                behandlingsstatus = TilbakekrevingHendelse.BehandlingStatus.OPPRETTET,
                forrigeBehandlingsstatus = null,
                totaltFeilutbetaltBeløp = 15000.toBigDecimal(),
                saksbehandlingURL = "https://tilbakekreving.intern.nav.no/behandling/$tilbakekrevingBehandlingId",
                fullstendigPeriode =
                    TilbakekrevingHendelse.Periode(
                        fom = LocalDate.parse("2025-01-01"),
                        tom = LocalDate.parse("2025-06-30"),
                    ),
            )
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
          "eksternBehandlingId": "$behandlingId",
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
