package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import org.junit.jupiter.api.Test

class ForslagTilVedtakMottakTest {
    private val testRapid = TestRapid()
    private val oppgaveMediator = mockk<OppgaveMediator>(relaxed = true)
    private val behandlingId = UUIDv7.ny()
    private val søknadId = UUIDv7.ny()
    private val ident = "123456678912"

    init {
        ForslagTilVedtakMottak(testRapid, oppgaveMediator)
    }

    @Test
    fun `Skal kunne motta forslag_til_vedtak hendelse med avslag minsteinntekt`() {
        testRapid.sendTestMessage(forslagTilVedtakAvslagMinsteinntektJson)

        val hendelse = slot<ForslagTilVedtakHendelse>()
        verify(exactly = 1) {
            oppgaveMediator.settOppgaveKlarTilBehandling(capture(hendelse))
        }
        hendelse.captured.ident shouldBe ident
        hendelse.captured.søknadId shouldBe søknadId
        hendelse.captured.behandlingId shouldBe behandlingId
        hendelse.captured.emneknagger shouldBe setOf("Avslag minsteinntekt")
    }

    @Test
    fun `Skal kunne motta forslag_til_vedtak hendelse med avslag som ikke er minsteinntekt`() {
        testRapid.sendTestMessage(forslagTilVedtakAvslagAlderJson)

        verify(exactly = 1) {
            val forslagTilVedtakHendelse =
                ForslagTilVedtakHendelse(
                    ident = ident,
                    søknadId = søknadId,
                    behandlingId = behandlingId,
                    emneknagger = setOf("Avslag"),
                )
            oppgaveMediator.settOppgaveKlarTilBehandling(forslagTilVedtakHendelse)
        }
    }

    @Test
    fun `Skal kunne motta forslag_til_vedtak hendelse med innvilgelse`() {
        testRapid.sendTestMessage(forslagTilVedtakInnvilgelseJson)

        verify(exactly = 1) {
            val forslagTilVedtakHendelse =
                ForslagTilVedtakHendelse(
                    ident = ident,
                    søknadId = søknadId,
                    behandlingId = behandlingId,
                    emneknagger = setOf("Innvilgelse"),
                )
            oppgaveMediator.settOppgaveKlarTilBehandling(forslagTilVedtakHendelse)
        }
    }

    //language=json
    private val forslagTilVedtakAvslagMinsteinntektJson =
        """
        {
          "@event_name": "forslag_til_vedtak",
          "prøvingsdato": "2024-12-01",
          "fastsatt": {
            "utfall": false
          },
          "vilkår": [
            {
              "navn": "Oppfyller kravet til alder",
              "status": "Oppfylt",
              "vurderingstidspunkt": "2024-12-19T14:09:57.269936",
              "hjemmel": "folketrygdloven § 4-23"
            },
            {
              "navn": "Krav til minsteinntekt",
              "status": "IkkeOppfylt",
              "vurderingstidspunkt": "2024-12-19T14:09:57.66249",
              "hjemmel": "folketrygdloven § 4-4"
            }
          ],
          "ident": "$ident",
          "behandlingId": "$behandlingId",
          "gjelderDato": "2024-11-19",
          "søknadId": "$søknadId",
          "søknad_uuid": "$søknadId"
        }
        """.trimIndent()

    //language=json
    private val forslagTilVedtakInnvilgelseJson =
        """
        {
            "@event_name": "forslag_til_vedtak",
            "prøvingsdato": "2024-12-01",
            "fastsatt": {
              "utfall": true
            },
            "vilkår": [
                {
                  "navn": "Oppfyller kravet til alder",
                  "status": "Oppfylt",
                  "vurderingstidspunkt": "2024-12-19T14:09:57.269936",
                  "hjemmel": "folketrygdloven § 4-23"
                },
                {
                  "navn": "Krav til minsteinntekt",
                  "status": "Oppfylt",
                  "vurderingstidspunkt": "2024-12-19T14:09:57.66249",
                  "hjemmel": "folketrygdloven § 4-4"
                }
            ],
            "ident": "$ident",
            "behandlingId": "$behandlingId",
            "gjelderDato": "2024-11-19",
            "søknadId": "$søknadId",
            "søknad_uuid": "$søknadId"
        }
        """.trimIndent()

    //language=json
    private val forslagTilVedtakAvslagAlderJson =
        """
        {
            "@event_name": "forslag_til_vedtak",
            "prøvingsdato": "2024-12-01",
            "fastsatt": {
              "utfall": false
            },
            "vilkår": [
            {
              "navn": "Oppfyller kravet til alder",
              "status": "IkkeOppfylt",
              "vurderingstidspunkt": "2024-12-19T14:09:57.269936",
              "hjemmel": "folketrygdloven § 4-23"
            },
            {
              "navn": "Krav til minsteinntekt",
              "status": "Oppfylt",
              "vurderingstidspunkt": "2024-12-19T14:09:57.66249",
              "hjemmel": "folketrygdloven § 4-4"
            }
          ],
            "ident": "$ident",
            "behandlingId": "$behandlingId",
            "gjelderDato": "2024-11-19",
            "søknadId": "$søknadId",
            "søknad_uuid": "$søknadId"
        }
        """.trimIndent()
}
