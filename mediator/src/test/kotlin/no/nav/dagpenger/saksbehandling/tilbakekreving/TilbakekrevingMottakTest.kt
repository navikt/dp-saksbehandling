package no.nav.dagpenger.saksbehandling.tilbakekreving

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.hendelser.TilbakekrevingHendelse
import no.nav.dagpenger.saksbehandling.tilbakekreving.Tilbakekreving.BehandlingStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class TilbakekrevingMottakTest {
    private val testRapid = TestRapid()
    private val tilbakekrevingMediator = mockk<TilbakekrevingMediator>()
    private val tilbakekrevingBehandlingId = UUID.randomUUID()
    private val behandlingId = UUID.randomUUID()

    init {
        every { tilbakekrevingMediator.håndter(any<TilbakekrevingHendelse>()) } just Runs
        TilbakekrevingMottak(
            rapidsConnection = testRapid,
            tilbakekrevingMediator = tilbakekrevingMediator,
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["TIL_FORHÅNDSVARSEL", "TIL_BEHANDLING", "TIL_GODKJENNING", "AVSLUTTET"])
    fun `Skal motta hendelse for alle statuser og kalle oppgaveMediator`(status: String) {
        testRapid.sendTestMessage(tilbakekrevingMelding(status))
        verify(exactly = 1) { tilbakekrevingMediator.håndter(any<TilbakekrevingHendelse>()) }
    }

    @Test
    fun `Skal parse tilbakekrevingHendelse korrekt med venter`() {
        val slot = slot<TilbakekrevingHendelse>()
        testRapid.sendTestMessage(tilbakekrevingMelding("TIL_FORHÅNDSVARSEL", venter = "2026-10-02"))
        verify(exactly = 1) { tilbakekrevingMediator.håndter(capture(slot)) }
        slot.captured.let { hendelse ->
            hendelse.eksternBehandlingId shouldBe behandlingId
            hendelse.hendelseOpprettet shouldBe LocalDateTime.parse("2024-06-01T10:00:00.223195031")
            hendelse.tilbakekreving shouldBe
                Tilbakekreving(
                    behandlingId = tilbakekrevingBehandlingId,
                    opprettet = LocalDateTime.parse("2024-05-20T08:00:00.208815"),
                    avventBehandlingTilDato = LocalDate.parse("2026-10-02"),
                    varselSendt = LocalDate.parse("2024-05-21"),
                    behandlingsstatus = BehandlingStatus.TIL_FORHÅNDSVARSEL,
                    forrigeBehandlingsstatus = null,
                    totaltFeilutbetaltBeløp = 15000.toBigDecimal(),
                    saksbehandlingURL = "https://tilbakekreving.intern.nav.no/behandling/$tilbakekrevingBehandlingId",
                    fullstendigPeriode =
                        Tilbakekreving.Periode(
                            fom = LocalDate.parse("2025-01-01"),
                            tom = LocalDate.parse("2025-06-30"),
                        ),
                )
        }
    }

    @Test
    fun `Skal parse tilbakekrevingHendelse korrekt uten venter`() {
        val slot = slot<TilbakekrevingHendelse>()
        testRapid.sendTestMessage(tilbakekrevingMelding("TIL_FORHÅNDSVARSEL"))
        verify(exactly = 1) { tilbakekrevingMediator.håndter(capture(slot)) }
        slot.captured.tilbakekreving.avventBehandlingTilDato shouldBe null
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
              "eksternBehandlingId": "$behandlingId",
              "hendelseOpprettet": "2024-06-01T10:00:00"
            }
            """.trimIndent(),
        )
        verify(exactly = 0) { tilbakekrevingMediator.håndter(any<TilbakekrevingHendelse>()) }
    }

    @Test
    fun `Skal ignorere meldinger som ikke har eksternBehandlingId i UUID format`() {
        testRapid.sendTestMessage(
            //language=json
            """
            {
              "hendelsestype": "behandling_endret",
              "tilbakekreving": {
                "behandlingsstatus": "OPPRETTET",
                "behandlingId": "$tilbakekrevingBehandlingId",
                "totaltFeilutbetaltBeløp": "15000"
              },
              "eksternBehandlingId": "ABC",
              "hendelseOpprettet": "2024-06-01T10:00:00"
            }
            """.trimIndent(),
        )
        verify(exactly = 0) { tilbakekrevingMediator.håndter(any<TilbakekrevingHendelse>()) }
    }

    //language=json
    private fun tilbakekrevingMelding(
        status: String,
        venter: String? = null,
    ): String {
        val venterJson = venter?.let { "\"venter\" : {\"grunn\": \"AVVENTER_BRUKERUTTALELSE\",\"gjenopptas\": \"$it\" }, " } ?: ""
        return """
            {
              "hendelsestype": "behandling_endret",
              "versjon": 1,
              "eksternBehandlingId": "$behandlingId",
              "hendelseOpprettet": "2024-06-01T10:00:00.223195031+02:00",
              "tilbakekreving": {
                "behandlingId": "$tilbakekrevingBehandlingId",
                "sakOpprettet": "2024-05-20T08:00:00.208815+02:00",
                $venterJson
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
}
