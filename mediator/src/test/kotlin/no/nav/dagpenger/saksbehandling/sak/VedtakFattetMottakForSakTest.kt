package no.nav.dagpenger.saksbehandling.sak

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
import org.junit.jupiter.api.Test

class VedtakFattetMottakForSakTest {
    private val testRapid = TestRapid()
    private val søknadId = UUIDv7.ny()
    private val behandlingId = UUIDv7.ny()
    private val ident = "12345612345"
    private val sakId = UUIDv7.ny()

    @Test
    fun `skal oppdatere merket er_dp_dak hvis vedtak_fattet gjelder innvilgelse av søknad`() {
        val hendelse = slot<VedtakFattetHendelse>()
        val sakMediatorMock =
            mockk<SakMediator>().also {
                every { it.merkSakenSomDpSak(any()) } just Runs
            }
        val sakRepositoryMock =
            mockk<SakRepository>().also {
                every { it.hentSakIdForBehandlingId(behandlingId) } returns sakId
            }
        VedtakFattetMottakForSak(
            rapidsConnection = testRapid,
            sakRepository = sakRepositoryMock,
            sakMediator = sakMediatorMock,
        )
        testRapid.sendTestMessage(vedtakFattetEvent())
        verify(exactly = 1) {
            sakMediatorMock.merkSakenSomDpSak(capture(hendelse))
        }
        hendelse.captured.ident shouldBe ident
        hendelse.captured.behandlingId shouldBe behandlingId
        hendelse.captured.behandletHendelseId shouldBe søknadId.toString()
        hendelse.captured.behandletHendelseType shouldBe "Søknad"
        hendelse.captured.sak.id shouldBe sakId.toString()
        hendelse.captured.sak.kontekst shouldBe "Dagpenger"
        hendelse.captured.automatiskBehandlet shouldBe false
    }

    @Test
    fun `Skal ikke markere er_dp_sak ved avslag på søknad`() {
        val sakMediatorMock = mockk<SakMediator>()
        val sakRepositoryMock = mockk<SakRepository>()
        VedtakFattetMottakForSak(
            rapidsConnection = testRapid,
            sakRepository = sakRepositoryMock,
            sakMediator = sakMediatorMock,
        )

        testRapid.sendTestMessage(vedtakFattetEvent(utfall = false))
        verify(exactly = 0) {
            sakMediatorMock.merkSakenSomDpSak(any())
        }
    }

    @Test
    fun `Skal ikke håndtere behandlinger som ikke er type Søknad`() {
        val sakMediatorMock = mockk<SakMediator>()
        val sakRepositoryMock = mockk<SakRepository>()
        VedtakFattetMottakForSak(
            rapidsConnection = testRapid,
            sakRepository = sakRepositoryMock,
            sakMediator = sakMediatorMock,
        )

        testRapid.sendTestMessage(vedtakFattetEvent(behandletHendelseType = "Meldekort"))
        verify(exactly = 0) {
            sakMediatorMock.merkSakenSomDpSak(any())
        }
    }

    private fun vedtakFattetEvent(
        ident: String = this.ident,
        behandlingId: String = this.behandlingId.toString(),
        søknadId: String = this.søknadId.toString(),
        utfall: Boolean = true,
        behandletHendelseType: String = "Søknad",
    ): String {
        return """
            {
                "@event_name": "vedtak_fattet",
                "ident": "$ident",
                "behandlingId": "$behandlingId",
                "behandletHendelse": {
                    "id": "$søknadId",
                    "type": "$behandletHendelseType"
                },
                "fastsatt": {
                    "utfall": $utfall
                },
                "automatisk": false
            }
            """.trimIndent()
    }
}
