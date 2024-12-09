package no.nav.dagpenger.saksbehandling.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
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

        verify(exactly = 1) {
            val forslagTilVedtakHendelse =
                ForslagTilVedtakHendelse(
                    ident = ident,
                    søknadId = søknadId,
                    behandlingId = behandlingId,
                    emneknagger = setOf("Avslag minsteinntekt"),
                )
            oppgaveMediator.settOppgaveKlarTilBehandling(forslagTilVedtakHendelse)
        }
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

    @Test
    fun `Skal kunne motta forslag_til_vedtak hendelse uten utfall`() {
        testRapid.sendTestMessage(forslagTilVedtakUtfallNullJson)

        verify(exactly = 1) {
            val forslagTilVedtakHendelse =
                ForslagTilVedtakHendelse(
                    ident = ident,
                    søknadId = søknadId,
                    behandlingId = behandlingId,
                    emneknagger = emptySet(),
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
            "utfall": false,
            "harAvklart": "Krav til minsteinntekt",
            "avklaringer": [{
                    "type": "InntektNesteKalendermåned",
                    "utfall": "Manuell",
                    "begrunnelse": "Personen har inntekter som tilhører neste inntektsperiode"
                }, {
                    "type": "SvangerskapsrelaterteSykepenger",
                    "utfall": "Manuell",
                    "begrunnelse": "Personen har sykepenger som kan være svangerskapsrelaterte"
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
            "utfall": true,
            "harAvklart": "Krav på dagpenger",
            "avklaringer": [{
                    "type": "SvangerskapsrelaterteSykepenger",
                    "utfall": "Manuell",
                    "begrunnelse": "Personen har sykepenger som kan være svangerskapsrelaterte"
                }, {
                    "type": "Totrinnskontroll",
                    "utfall": "Manuell",
                    "begrunnelse": "Totrinnskontroll"
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
            "utfall": false,
            "harAvklart": "Krav til alder",
            "ident": "$ident",
            "behandlingId": "$behandlingId",
            "gjelderDato": "2024-11-19",
            "søknadId": "$søknadId",
            "søknad_uuid": "$søknadId"
        }
        """.trimIndent()

    //language=json
    private val forslagTilVedtakUtfallNullJson =
        """
        {
            "@event_name": "forslag_til_vedtak",
            "prøvingsdato": "2024-12-01",
            "utfall": null,
            "harAvklart": "Mikke mus",
            "ident": "$ident",
            "behandlingId": "$behandlingId",
            "gjelderDato": "2024-11-19",
            "søknadId": "$søknadId",
            "søknad_uuid": "$søknadId"
        }
        """.trimIndent()
}
