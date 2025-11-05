package no.nav.dagpenger.saksbehandling.henvendelse

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.KlageMediator
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.TestHelper
import no.nav.dagpenger.saksbehandling.behandling.BehandlingKlient
import no.nav.dagpenger.saksbehandling.hendelser.Aksjon
import no.nav.dagpenger.saksbehandling.hendelser.FerdigstillHenvendelseHendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlageMottattHendelse
import org.junit.jupiter.api.Test
import java.util.UUID

class HenvendelseBehandlerTest {
    private val saksbehandler = Saksbehandler("Z123456", emptySet())
    private val testHenvendelse = TestHelper.testHenvendelse
    private val testOppgave = TestHelper.testOppgave

    @Test
    fun `Behandle en henvendelse med aksjon av type Avslutt `() {
        val henvendelseBehandler =
            HenvendelseBehandler(
                klageMediator = mockk(),
                behandlingKlient = mockk(),
            )

        henvendelseBehandler.utførAksjon(lagHendelse(Aksjon.Avslutt), testHenvendelse).let {
            it.henvendelseId shouldBe testHenvendelse.henvendelseId
            it.aksjon shouldBe "Avslutt"
            it.behandlingId shouldBe null
            it.utførtAv shouldBe saksbehandler
        }
    }

    @Test
    fun `Behandle en henvendelse med aksjon av type OpprettKlage`() {
        val testSakId = UUID.randomUUID()

        val slot = slot<KlageMottattHendelse>()
        val klageMediator =
            mockk<KlageMediator>().also {
                every {
                    it.opprettKlage(capture(slot))
                } returns testOppgave
            }
        val henvendelseBehandler =
            HenvendelseBehandler(
                klageMediator = klageMediator,
                behandlingKlient = mockk(),
            )

        henvendelseBehandler.utførAksjon(
            hendelse = lagHendelse(Aksjon.OpprettKlage(sakId = testSakId)),
            henvendelse = testHenvendelse,
        )
            .let {
                it.henvendelseId shouldBe testHenvendelse.henvendelseId
                it.aksjon shouldBe "OpprettKlage"
                it.behandlingId shouldBe testOppgave.behandling.behandlingId
                it.utførtAv shouldBe saksbehandler
            }

        with(slot.captured) {
            ident shouldBe testHenvendelse.person.ident
            opprettet shouldBe testHenvendelse.mottatt
            journalpostId shouldBe testHenvendelse.journalpostId
            sakId shouldBe testSakId
        }
    }

    @Test
    fun `Behandle en henvendelse med aksjon av type OpprettManuellBehandling`() {
        val saksbehandlerToken = "token"
        val behandlingId = UUID.randomUUID()

        val behandlingKlient =
            mockk<BehandlingKlient>().also {
                every {
                    it.opprettManuellBehandling(
                        personIdent = testHenvendelse.person.ident,
                        saksbehandlerToken = saksbehandlerToken,
                    )
                } returns Result.success(behandlingId)
            }
        val henvendelseBehandler =
            HenvendelseBehandler(
                klageMediator = mockk<KlageMediator>(),
                behandlingKlient = behandlingKlient,
            )

        henvendelseBehandler.utførAksjon(
            hendelse = lagHendelse(Aksjon.OpprettManuellBehandling(saksbehandlerToken = saksbehandlerToken)),
            henvendelse = testHenvendelse,
        )
            .let {
                it.henvendelseId shouldBe testHenvendelse.henvendelseId
                it.aksjon shouldBe "OpprettManuellBehandling"
                it.behandlingId shouldBe behandlingId
                it.utførtAv shouldBe saksbehandler
            }

        verify(exactly = 1) {
            behandlingKlient.opprettManuellBehandling(testHenvendelse.person.ident, saksbehandlerToken)
        }
    }

    private fun lagHendelse(aksjon: Aksjon): FerdigstillHenvendelseHendelse {
        return FerdigstillHenvendelseHendelse(
            henvendelseId = testHenvendelse.henvendelseId,
            aksjon = aksjon,
            utførtAv = saksbehandler,
        )
    }
}
