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
import no.nav.dagpenger.saksbehandling.helper.behandlingResultatEvent
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import org.junit.jupiter.api.Test

class BehandlingsResultatMottakForSakTest {
    private val testRapid = TestRapid()
    private val søknadId = UUIDv7.ny()
    private val behandlingId = UUIDv7.ny()
    private val ident = "12345612345"
    private val sakId = UUIDv7.ny()

    @Test
    fun `skal oppdatere merket er_dp_sak hvis vedtak gjelder innvilgelse av søknad`() {
        val hendelse = slot<VedtakFattetHendelse>()
        val sakMediatorMock =
            mockk<SakMediator>().also {
                every { it.merkSakenSomDpSak(any()) } just Runs
            }
        val sakRepositoryMock =
            mockk<SakRepository>().also {
                every { it.hentSakIdForBehandlingId(behandlingId) } returns sakId
            }
        BehandlingsResultatMottakForSak(
            rapidsConnection = testRapid,
            sakRepository = sakRepositoryMock,
            sakMediator = sakMediatorMock,
        )
        testRapid.sendTestMessage(behandlingResultat())
        verify(exactly = 1) {
            sakMediatorMock.merkSakenSomDpSak(capture(hendelse))
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
    fun `Skal ikke markere er_dp_sak ved avslag på søknad`() {
        val sakMediatorMock = mockk<SakMediator>()
        val sakRepositoryMock = mockk<SakRepository>()
        BehandlingsResultatMottakForSak(
            rapidsConnection = testRapid,
            sakRepository = sakRepositoryMock,
            sakMediator = sakMediatorMock,
        )

        testRapid.sendTestMessage(behandlingResultat(harRett = false))
        verify(exactly = 0) {
            sakMediatorMock.merkSakenSomDpSak(any())
        }
    }

    @Test
    fun `Skal ikke håndtere behandlinger som ikke er type Søknad`() {
        val sakMediatorMock = mockk<SakMediator>()
        val sakRepositoryMock = mockk<SakRepository>()
        BehandlingsResultatMottakForSak(
            rapidsConnection = testRapid,
            sakRepository = sakRepositoryMock,
            sakMediator = sakMediatorMock,
        )

        testRapid.sendTestMessage(behandlingResultat(behandletHendelseType = "Meldekort"))
        verify(exactly = 0) {
            sakMediatorMock.merkSakenSomDpSak(any())
        }
    }

    private fun behandlingResultat(
        ident: String = this.ident,
        behandlingId: String = this.behandlingId.toString(),
        søknadId: String = this.søknadId.toString(),
        behandletHendelseType: String = "Søknad",
        harRett: Boolean = true,
    ): String {
        return behandlingResultatEvent(
            ident = ident,
            behandlingId = behandlingId,
            søknadId = søknadId,
            behandletHendelseType = behandletHendelseType,
            harRett = harRett,
        )
    }
}
