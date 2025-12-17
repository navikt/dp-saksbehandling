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
import no.nav.dagpenger.saksbehandling.helper.behandlingsresultatEvent
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import org.junit.jupiter.api.Test
import java.util.UUID

class BehandlingsresultatMottakForSakTest {
    private val testRapid = TestRapid()
    private val søknadId = UUIDv7.ny()
    private val behandlingId = UUIDv7.ny()
    private val ident = "12345612345"
    private val sakId = UUIDv7.ny()

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `skal oppdatere merket er_dp_sak og  publisere melding om vedtak fattet utenfor Arena hvis vedtak gjelder innvilgelse av søknad`() {
        val hendelse = slot<VedtakFattetHendelse>()
        val sakMediatorMock =
            mockk<SakMediator>().also {
                every { it.merkSakenSomDpSak(any()) } just Runs
            }
        val sakRepositoryMock =
            mockk<SakRepository>().also {
                every { it.hentSakIdForBehandlingId(behandlingId) } returns sakId
            }
        BehandlingsresultatMottakForSak(
            rapidsConnection = testRapid,
            sakRepository = sakRepositoryMock,
            sakMediator = sakMediatorMock,
        )
        testRapid.sendTestMessage(
            behandlingsresultatJson(),
        )
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
    fun `Skal ikke markere er_dp_sak deroms behandling resultat er innvilgelse av gjenopptak`() {
        val sakMediatorMock = mockk<SakMediator>()
        val sakRepositoryMock = mockk<SakRepository>()
        BehandlingsresultatMottakForSak(
            rapidsConnection = testRapid,
            sakRepository = sakRepositoryMock,
            sakMediator = sakMediatorMock,
        )

        testRapid.sendTestMessage(behandlingsresultatJson(harRett = true, basertPå = UUIDv7.ny()))

        verify(exactly = 0) {
            sakMediatorMock.merkSakenSomDpSak(any())
        }
        testRapid.inspektør.size shouldBe 0
    }

    @Test
    fun `Skal ikke markere er_dp_sak ved avslag på søknad`() {
        val sakMediatorMock = mockk<SakMediator>()
        val sakRepositoryMock = mockk<SakRepository>()
        BehandlingsresultatMottakForSak(
            rapidsConnection = testRapid,
            sakRepository = sakRepositoryMock,
            sakMediator = sakMediatorMock,
        )

        testRapid.sendTestMessage(behandlingsresultatJson(harRett = false))

        verify(exactly = 0) {
            sakMediatorMock.merkSakenSomDpSak(any())
        }
        testRapid.inspektør.size shouldBe 0
    }

    @Test
    fun `Skal ikke håndtere behandlinger som ikke er type Søknad`() {
        val sakMediatorMock = mockk<SakMediator>()
        val sakRepositoryMock = mockk<SakRepository>()
        BehandlingsresultatMottakForSak(
            rapidsConnection = testRapid,
            sakRepository = sakRepositoryMock,
            sakMediator = sakMediatorMock,
        )

        testRapid.sendTestMessage(behandlingsresultatJson(behandletHendelseType = "Meldekort"))

        verify(exactly = 0) {
            sakMediatorMock.merkSakenSomDpSak(any())
        }
        testRapid.inspektør.size shouldBe 0
    }

    private fun behandlingsresultatJson(
        ident: String = this.ident,
        behandlingId: String = this.behandlingId.toString(),
        søknadId: String = this.søknadId.toString(),
        behandletHendelseType: String = "Søknad",
        harRett: Boolean = true,
        basertPå: UUID? = null,
    ): String =
        behandlingsresultatEvent(
            ident = ident,
            behandlingId = behandlingId,
            behandletHendelseId = søknadId,
            behandletHendelseType = behandletHendelseType,
            harRett = harRett,
            basertPå = basertPå,
        )
}
