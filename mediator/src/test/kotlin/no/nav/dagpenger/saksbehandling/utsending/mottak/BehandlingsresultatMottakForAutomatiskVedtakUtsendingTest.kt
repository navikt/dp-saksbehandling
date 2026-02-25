package no.nav.dagpenger.saksbehandling.utsending.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.UtsendingSak
import no.nav.dagpenger.saksbehandling.db.sak.SakRepository
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator
import org.junit.jupiter.api.Test

class BehandlingsresultatMottakForAutomatiskVedtakUtsendingTest {
    private val testRapid = TestRapid()
    private val søknadId = UUIDv7.ny()
    private val behandlingId = UUIDv7.ny()
    private val ident = "12345612345"
    private val sakId = UUIDv7.ny()

    @Test
    fun `Skal håndtere behandlingsresultat som er automatisk behandlet`() {
        val utsendingMediatorMock = mockk<UtsendingMediator>(relaxed = true)

        BehandlingsresultatMottakForAutomatiskVedtakUtsending(
            rapidsConnection = testRapid,
            utsendingMediator = utsendingMediatorMock,
            sakRepository =
                mockk<SakRepository>().also {
                    every { it.hentSakIdForBehandlingId(behandlingId) } returns sakId
                },
        )

        testRapid.sendTestMessage(
            //language=JSON
            """
            {
              "@event_name": "behandlingsresultat",
              "ident": "$ident",
              "behandlingId": "$behandlingId",
              "behandletHendelse": {
                "id": "$søknadId",
                "type": "Søknad"
              },
              "automatisk": true,
              "rettighetsperioder": [
                {
                  "fraOgMed": "2025-09-09",
                  "harRett": false
                }
              ]
            }
            """.trimIndent(),
        )

        verify(exactly = 1) {
            utsendingMediatorMock.startUtsendingForAutomatiskVedtakFattet(
                VedtakFattetHendelse(
                    behandlingId = behandlingId,
                    behandletHendelseId = søknadId.toString(),
                    behandletHendelseType = "Søknad",
                    ident = ident,
                    sak =
                        UtsendingSak(
                            id = sakId.toString(),
                            kontekst = "Dagpenger",
                        ),
                    automatiskBehandlet = true,
                ),
            )
        }
    }

    @Test
    fun `Skal ikke håndtere behandlingsresultat som er manuelt behandlet`() {
        val utsendingMediatorMock = mockk<UtsendingMediator>(relaxed = true)

        BehandlingsresultatMottakForAutomatiskVedtakUtsending(
            rapidsConnection = testRapid,
            utsendingMediator = utsendingMediatorMock,
            sakRepository =
                mockk<SakRepository>().also {
                    every { it.hentSakIdForBehandlingId(behandlingId) } returns sakId
                },
        )

        testRapid.sendTestMessage(
            //language=JSON
            """
            {
              "@event_name": "behandlingsresultat",
              "ident": "$ident",
              "behandlingId": "$behandlingId",
              "behandletHendelse": {
                "id": "$søknadId",
                "type": "Søknad"
              },
              "automatisk": false,
              "rettighetsperioder": [
                {
                  "fraOgMed": "2025-09-09",
                  "harRett": false
                }
              ]
            }
            """.trimIndent(),
        )

        verify(exactly = 0) {
            utsendingMediatorMock.startUtsendingForAutomatiskVedtakFattet(any())
        }
    }
}
