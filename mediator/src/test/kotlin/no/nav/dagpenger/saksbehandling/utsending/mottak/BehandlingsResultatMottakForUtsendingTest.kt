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
import no.nav.dagpenger.saksbehandling.db.sak.SakRepository
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator
import org.junit.jupiter.api.Test

class BehandlingsResultatMottakForUtsendingTest {
    private val testRapid = TestRapid()
    private val søknadId = UUIDv7.ny()
    private val behandlingId = UUIDv7.ny()
    private val ident = "12345612345"
    private val sakId = UUIDv7.ny()

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `Skal starte utsending og publisere melding om vedtak fattet utenfor Arena dersom behandling resultat er basert på en søknad og skal tilhøre dp-sak`() {
        val hendelse = slot<VedtakFattetHendelse>()
        val utsendingMediatorMock =
            mockk<UtsendingMediator>().also {
                every { it.startUtsendingForVedtakFattet(any()) } just Runs
            }

        val sakRepositoryMock =
            mockk<SakRepository>().also {
                every { it.hentSakIdForBehandlingId(behandlingId) } returns sakId
            }
        BehandlingsResultatMottakForUtsending(
            rapidsConnection = testRapid,
            utsendingMediator = utsendingMediatorMock,
            sakRepository = sakRepositoryMock,
        )

        testRapid.sendTestMessage(behandlingResultatEvent())
        verify(exactly = 1) {
            utsendingMediatorMock.startUtsendingForVedtakFattet(capture(hendelse))
        }
        hendelse.captured.ident shouldBe ident
        hendelse.captured.behandlingId shouldBe behandlingId
        hendelse.captured.behandletHendelseId shouldBe søknadId.toString()
        hendelse.captured.behandletHendelseType shouldBe "Søknad"
        hendelse.captured.sak.id shouldBe sakId.toString()
        hendelse.captured.sak.kontekst shouldBe "Dagpenger"
        hendelse.captured.automatiskBehandlet shouldBe false

        testRapid.inspektør.size shouldBe 1
        testRapid.inspektør.message(0).also { message ->
            message["@event_name"].asText() shouldBe "vedtak_fattet_utenfor_arena"
            message["behandlingId"].asText() shouldBe behandlingId.toString()
            message["søknadId"].asText() shouldBe søknadId.toString()
            message["sakId"].asText() shouldBe sakId.toString()
            message["ident"].asText() shouldBe ident
        }
    }

    @Test
    fun `Skal ikke håndtere avslag på søknad `() {
        val utsendingMediatorMock = mockk<UtsendingMediator>()

        VedtakFattetMottakForUtsending(
            rapidsConnection = testRapid,
            utsendingMediator = utsendingMediatorMock,
            sakRepository = mockk<SakRepository>(),
        )

        testRapid.sendTestMessage(behandlingResultatEvent(harRett = false))

        verify(exactly = 0) {
            utsendingMediatorMock.startUtsendingForVedtakFattet(any())
        }
    }

    @Test
    fun `Skal ikke håndtere behandlinger som ikke er type Søknad `() {
        val utsendingMediatorMock = mockk<UtsendingMediator>()

        VedtakFattetMottakForUtsending(
            rapidsConnection = testRapid,
            utsendingMediator = utsendingMediatorMock,
            sakRepository = mockk<SakRepository>(),
        )

        testRapid.sendTestMessage(behandlingResultatEvent(behandletHendelseType = "Meldekort"))

        verify(exactly = 0) {
            utsendingMediatorMock.startUtsendingForVedtakFattet(any())
        }
    }

    @Test
    fun `Skal ikke håndtere behandlinger med flere rettighetsperiioder `() {
        val utsendingMediatorMock = mockk<UtsendingMediator>()

        VedtakFattetMottakForUtsending(
            rapidsConnection = testRapid,
            utsendingMediator = utsendingMediatorMock,
            sakRepository = mockk<SakRepository>(),
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
                  "harRett": true
                }
                {
                  "fraOgMed": "2025-09-09",
                  "harRett": true
                }
              ]
            }
            """.trimIndent(),
        )

        verify(exactly = 0) {
            utsendingMediatorMock.startUtsendingForVedtakFattet(any())
        }
    }

    private fun behandlingResultatEvent(
        ident: String = this.ident,
        behandlingId: String = this.behandlingId.toString(),
        søknadId: String = this.søknadId.toString(),
        behandletHendelseType: String = "Søknad",
        harRett: Boolean = true,
    ): String {
        //language=JSON
        return """
            {
              "@event_name": "behandlingsresultat",
              "ident": "$ident",
              "behandlingId": "$behandlingId",
              "behandletHendelse": {
                "id": "$søknadId",
                "type": "$behandletHendelseType"
              },
              "automatisk": false,
              "rettighetsperioder": [
                {
                  "fraOgMed": "2025-09-09",
                  "harRett": $harRett
                }
              ]
            }
            """.trimIndent()
    }
}
