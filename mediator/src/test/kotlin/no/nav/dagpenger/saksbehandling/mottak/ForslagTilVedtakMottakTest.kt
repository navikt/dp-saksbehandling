package no.nav.dagpenger.saksbehandling.mottak

import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test

class ForslagTilVedtakMottakTest {
    private val testRapid = TestRapid()
    private val oppgaveMediator = mockk<OppgaveMediator>(relaxed = true)

    init {
        ForslagTilVedtakMottak(testRapid, oppgaveMediator)
    }

    @Test
    fun `Skal kunne motta forslag til vedtak events`() {
        testRapid.sendTestMessage(forslagTilVedtakJson)
        verify(exactly = 1) {
            oppgaveMediator.settOppgaveKlarTilBehandling(any<ForslagTilVedtakHendelse>())
        }
    }

    //language=json
    val forslagTilVedtakJson =
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
            "ident": "123456678912",
            "behandlingId": "018f6195-5fb3-7116-884f-f5b3a8718560",
            "gjelderDato": "2024-05-10",
            "søknadId": "4afce924-6cb4-4ab4-a92b-fe91e24f31bf",
            "søknad_uuid": "4afce924-6cb4-4ab4-a92b-fe91e24f31bf",
            "opprettet": "2024-05-10T10:18:51.251369",
            "@id": "3b0fab42-74af-423c-a04f-a0b7562d2d7b",
            "@opprettet": "2024-05-10T10:18:51.29592"
        }
        """.trimIndent()
}
