package no.nav.dagpenger.saksbehandling.mottak

import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.helse.rapids_rivers.testsupport.TestRapid
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
    fun `Skal kunne motta forslag til vedtak events`() {
        testRapid.sendTestMessage(forslagTilVedtakJson)

        verify(exactly = 1) {
            val forslagTilVedtakHendelse =
                ForslagTilVedtakHendelse(
                    ident = ident,
                    søknadId = søknadId,
                    behandlingId = behandlingId,
                    emneknagger = setOf("EØSArbeid", "HarRapportertInntektNesteMåned", "SykepengerSiste36Måneder"),
                )
            oppgaveMediator.settOppgaveKlarTilBehandling(forslagTilVedtakHendelse)
        }
    }

    //language=json
    private val forslagTilVedtakJson =
        """
          {
            "@event_name": "forslag_til_vedtak",
            "utfall": false,
            "harAvklart": "Krav på dagpenger",
            "avklaringer": [
              {
                "type": "EØSArbeid",
                "utfall": "Manuell",
                "begrunnelse": "Personen har oppgitt arbeid fra EØS"
              },
              {
                "type": "HarRapportertInntektNesteMåned",
                "utfall": "Manuell",
                "begrunnelse": "Personen har inntekter som tilhører neste inntektsperiode"
              },
              {
                "type": "SykepengerSiste36Måneder",
                "utfall": "Manuell",
                "begrunnelse": "Personen har sykepenger som kan være svangerskapsrelaterte"
              }
            ],
            "ident": "$ident",
            "behandlingId": "$behandlingId",
            "gjelderDato": "2024-05-10",
            "søknadId": "$søknadId",
            "søknad_uuid": "$søknadId",
            "opprettet": "2024-05-10T10:18:51.251369",
            "@id": "3b0fab42-74af-423c-a04f-a0b7562d2d7b",
            "@opprettet": "2024-05-10T10:18:51.29592"
        }
        """.trimIndent()
}
