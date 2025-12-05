package no.nav.dagpenger.saksbehandling.utsending.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.UtsendingSak
import no.nav.dagpenger.saksbehandling.db.sak.SakRepository
import no.nav.dagpenger.saksbehandling.helper.behandlingResultatEvent
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator
import org.junit.jupiter.api.Test

class BehandlingsresultatMottakForUtsendingTest {
    private val testRapid = TestRapid()
    private val søknadId = UUIDv7.ny()
    private val behandlingId = UUIDv7.ny()
    private val ident = "12345612345"
    private val sakId = UUIDv7.ny()

    @Test
    fun `Skal starte utsending behandling resultat er basert på en søknad og skal tilhøre dp-sak`() {
        val hendelse = slot<VedtakFattetHendelse>()
        val utsendingMediatorMock =
            mockk<UtsendingMediator>().also {
                every { it.startUtsendingForVedtakFattet(any()) } just Runs
            }
        val sakRepositoryMock =
            mockk<SakRepository>().also {
                every { it.hentSakIdForBehandlingId(behandlingId) } returns sakId
                every { it.hentDagpengerSakIdForBehandlingId(any()) } throws RuntimeException()
            }
        BehandlingsresultatMottakForUtsending(
            rapidsConnection = testRapid,
            utsendingMediator = utsendingMediatorMock,
            sakRepository = sakRepositoryMock,
        )

        testRapid.sendTestMessage(behandlingResultat())
        verify(exactly = 1) {
            utsendingMediatorMock.startUtsendingForVedtakFattet(capture(hendelse))
        }
        hendelse.captured.ident shouldBe ident
        hendelse.captured.behandlingId shouldBe behandlingId
        hendelse.captured.behandletHendelseId shouldBe søknadId.toString()
        hendelse.captured.behandletHendelseType shouldBe "Søknad"
        hendelse.captured.sak.let {
            require(it != null)
            it.id shouldBe sakId.toString()
            it.kontekst shouldBe "Dagpenger"
        }
        hendelse.captured.automatiskBehandlet shouldBe false
    }

    @Test
    fun `Skal ikke håndtere avslag på søknad`() {
        val utsendingMediatorMock = mockk<UtsendingMediator>()

        BehandlingsresultatMottakForUtsending(
            rapidsConnection = testRapid,
            utsendingMediator = utsendingMediatorMock,
            sakRepository = mockk<SakRepository>(),
        )

        testRapid.sendTestMessage(behandlingResultat(harRett = false))

        verify(exactly = 0) {
            utsendingMediatorMock.startUtsendingForVedtakFattet(any())
        }
    }

    @Test
    fun `Skal håndtere meldekort-behandling som tilhører dp-sak`() {
        val utsendingMediatorMock =
            mockk<UtsendingMediator>().also {
                every { it.startUtsendingForVedtakFattet(any()) } just Runs
            }
        val sakRepositoryMock =
            mockk<SakRepository>().also {
                every { it.hentDagpengerSakIdForBehandlingId(any()) } returns sakId
                every { it.hentSakIdForBehandlingId(any()) } returns sakId
            }
        BehandlingsresultatMottakForUtsending(
            rapidsConnection = testRapid,
            utsendingMediator = utsendingMediatorMock,
            sakRepository = sakRepositoryMock,
        )

        testRapid.sendTestMessage(behandlingResultat(behandletHendelseType = "Meldekort"))

        verify(exactly = 1) {
            utsendingMediatorMock.startUtsendingForVedtakFattet(any())
        }
    }

    @Test
    fun `Skal håndtere manuell behandling som tilhører dp-sak`() {
        val utsendingMediatorMock =
            mockk<UtsendingMediator>().also {
                every { it.startUtsendingForVedtakFattet(any()) } just Runs
            }
        val sakRepositoryMock =
            mockk<SakRepository>().also {
                every { it.hentDagpengerSakIdForBehandlingId(any()) } returns sakId
                every { it.hentSakIdForBehandlingId(any()) } returns sakId
            }
        BehandlingsresultatMottakForUtsending(
            rapidsConnection = testRapid,
            utsendingMediator = utsendingMediatorMock,
            sakRepository = sakRepositoryMock,
        )

        testRapid.sendTestMessage(behandlingResultat(behandletHendelseType = "Manuell"))

        verify(exactly = 1) {
            utsendingMediatorMock.startUtsendingForVedtakFattet(any())
        }
    }

    @Test
    fun `Skal håndtere behandlinger med flere rettighetsperioder `() {
        val utsendingMediatorMock = mockk<UtsendingMediator>(relaxed = true)

        BehandlingsresultatMottakForUtsending(
            rapidsConnection = testRapid,
            utsendingMediator = utsendingMediatorMock,
            sakRepository =
                mockk<SakRepository>().also {
                    every { it.hentDagpengerSakIdForBehandlingId(behandlingId) } returns sakId
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
                },
                {
                  "fraOgMed": "2025-09-09",
                  "harRett": true
                }
              ]
            }
            """.trimIndent(),
        )

        verify(exactly = 1) {
            utsendingMediatorMock.startUtsendingForVedtakFattet(
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
                    automatiskBehandlet = false,
                ),
            )
        }
    }

    private fun behandlingResultat(
        ident: String = this.ident,
        behandlingId: String = this.behandlingId.toString(),
        søknadId: String = this.søknadId.toString(),
        behandletHendelseType: String = "Søknad",
        harRett: Boolean = true,
    ): String =
        behandlingResultatEvent(
            ident = ident,
            behandlingId = behandlingId,
            behandletHendelseId = søknadId,
            behandletHendelseType = behandletHendelseType,
            harRett = harRett,
        )
}
