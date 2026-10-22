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
    fun `Skal sette er_dp_sak true og publisere melding om vedtak fattet utenfor Arena ved første vedtak i brukers første sak i DP-sak`() {
        val hendelse = slot<VedtakFattetHendelse>()
        val sakMediatorMock =
            mockk<SakMediator>().also {
                every { it.merkSakenSomDpSak(any()) } just Runs
            }
        val sakRepositoryMock =
            mockk<SakRepository>().also {
                every { it.hentSakIdForBehandlingId(behandlingId) } returns sakId
                every { it.finnSisteDagpengeSakId(ident) } returns null
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
            message["@event_name"].stringValue() shouldBe "vedtak_fattet_utenfor_arena"
            message["behandlingId"].stringValue() shouldBe behandlingId.toString()
            message["søknadId"].stringValue() shouldBe søknadId.toString()
            message["sakId"].stringValue() shouldBe sakId.toString()
            message["ident"].stringValue() shouldBe ident
            message["førteTil"].stringValue() shouldBe "Innvilgelse"
        }
    }

    @Test
    @Suppress("ktlint:standard:max-line-length")
    fun `Skal publisere melding om vedtak fattet i ny sak når første vedtak fattes i brukers sak i DP-sak og det ikke er den første DP-saken`() {
        val hendelse = slot<VedtakFattetHendelse>()
        val sakMediatorMock =
            mockk<SakMediator>().also {
                every { it.merkSakenSomDpSak(any()) } just Runs
            }
        val sakRepositoryMock =
            mockk<SakRepository>().also {
                every { it.hentSakIdForBehandlingId(behandlingId) } returns sakId
                every { it.finnSisteDagpengeSakId(ident) } returns UUIDv7.ny()
            }
        BehandlingsresultatMottakForSak(
            rapidsConnection = testRapid,
            sakRepository = sakRepositoryMock,
            sakMediator = sakMediatorMock,
        )
        testRapid.sendTestMessage(
            behandlingsresultatJson(),
        )
        verify(exactly = 0) {
            sakMediatorMock.merkSakenSomDpSak(capture(hendelse))
        }
        testRapid.inspektør.size shouldBe 1
        testRapid.inspektør.message(0).also { message ->
            message["@event_name"].stringValue() shouldBe "vedtak_fattet_i_ny_sak"
            message["behandlingId"].stringValue() shouldBe behandlingId.toString()
            message["søknadId"].stringValue() shouldBe søknadId.toString()
            message["sakId"].stringValue() shouldBe sakId.toString()
            message["ident"].stringValue() shouldBe ident
            message["førteTil"].stringValue() shouldBe "Innvilgelse"
        }
    }

    @Test
    fun `Skal ikke markere er_dp_sak dersom behandling resultat er innvilgelse av gjenopptak`() {
        val sakMediatorMock = mockk<SakMediator>()
        val sakRepositoryMock = mockk<SakRepository>()
        BehandlingsresultatMottakForSak(
            rapidsConnection = testRapid,
            sakRepository = sakRepositoryMock,
            sakMediator = sakMediatorMock,
        )

        testRapid.sendTestMessage(
            behandlingsresultatJson(hendelseType = "Søknad", harRett = true, førteTil = "Innvilgelse", basertPå = UUIDv7.ny()),
        )

        verify(exactly = 0) {
            sakMediatorMock.merkSakenSomDpSak(any())
        }
        testRapid.inspektør.size shouldBe 0
    }

    @Test
    fun `Skal ikke markere er_dp_sak dersom behandling resultat er avslag av gjenopptak`() {
        val sakMediatorMock = mockk<SakMediator>()
        val sakRepositoryMock = mockk<SakRepository>()
        BehandlingsresultatMottakForSak(
            rapidsConnection = testRapid,
            sakRepository = sakRepositoryMock,
            sakMediator = sakMediatorMock,
        )

        testRapid.sendTestMessage(
            behandlingsresultatJson(hendelseType = "Søknad", harRett = false, førteTil = "Avslag", basertPå = UUIDv7.ny()),
        )

        verify(exactly = 0) {
            sakMediatorMock.merkSakenSomDpSak(any())
        }
        testRapid.inspektør.size shouldBe 0
    }

    @Test
    fun `Skal markere er_dp_sak og publisere melding om vedtak fattet utenfor Arena ved avslag på ny søknad`() {
        val hendelse = slot<VedtakFattetHendelse>()
        val sakMediatorMock =
            mockk<SakMediator>().also {
                every { it.merkSakenSomDpSak(any()) } just Runs
            }
        val sakRepositoryMock =
            mockk<SakRepository>().also {
                every { it.hentSakIdForBehandlingId(behandlingId) } returns sakId
                every { it.finnSisteDagpengeSakId(ident) } returns null
            }
        BehandlingsresultatMottakForSak(
            rapidsConnection = testRapid,
            sakRepository = sakRepositoryMock,
            sakMediator = sakMediatorMock,
        )

        testRapid.sendTestMessage(behandlingsresultatJson(hendelseType = "Søknad", harRett = false, førteTil = "Avslag"))

        verify(exactly = 1) {
            sakMediatorMock.merkSakenSomDpSak(capture(hendelse))
        }
        hendelse.captured.ident shouldBe ident
        hendelse.captured.behandlingId shouldBe behandlingId
        hendelse.captured.sak.let {
            require(it != null)
            it.id shouldBe sakId.toString()
        }

        testRapid.inspektør.size shouldBe 1
        testRapid.inspektør.message(0).also { message ->
            message["@event_name"].stringValue() shouldBe "vedtak_fattet_utenfor_arena"
            message["behandlingId"].stringValue() shouldBe behandlingId.toString()
            message["sakId"].stringValue() shouldBe sakId.toString()
            message["ident"].stringValue() shouldBe ident
            message["førteTil"].stringValue() shouldBe "Avslag"
        }
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

        testRapid.sendTestMessage(behandlingsresultatJson(hendelseType = "Meldekort"))

        verify(exactly = 0) {
            sakMediatorMock.merkSakenSomDpSak(any())
        }
        testRapid.inspektør.size shouldBe 0
    }

    private fun behandlingsresultatJson(
        ident: String = this.ident,
        behandlingId: String = this.behandlingId.toString(),
        søknadId: String = this.søknadId.toString(),
        hendelseType: String = "Søknad",
        harRett: Boolean = true,
        basertPå: UUID? = null,
        førteTil: String = "Innvilgelse",
    ): String =
        behandlingsresultatEvent(
            ident = ident,
            behandlingId = behandlingId,
            behandletHendelseId = søknadId,
            behandletHendelseType = hendelseType,
            harRett = harRett,
            basertPå = basertPå,
            førteTil = førteTil,
        )
}
