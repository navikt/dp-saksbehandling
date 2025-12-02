package no.nav.dagpenger.saksbehandling.innsending

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.KlageMediator
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.TestHelper
import no.nav.dagpenger.saksbehandling.behandling.BehandlingKlient
import no.nav.dagpenger.saksbehandling.hendelser.FerdigstillInnsendingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlageMottattHendelse
import org.junit.jupiter.api.Test
import java.util.UUID

class InnsendingBehandlerTest {
    private val saksbehandler = Saksbehandler("Z123456", emptySet())
    private val testInnsending = TestHelper.testInnsending
    private val testOppgave = TestHelper.testOppgave

    @Test
    fun `Behandle en innsending med aksjon av type Avslutt `() {
        val innsendingBehandler =
            InnsendingBehandler(
                klageMediator = mockk(),
                behandlingKlient = mockk(),
            )

        innsendingBehandler.utførAksjon(
            hendelse =
                lagHendelse(
                    aksjon = Aksjon.Avslutt(null),
                    vurdering = "Dette er en vurdering",
                ),
            innsending = testInnsending,
        ).let {
            it.innsendingId shouldBe testInnsending.innsendingId
            it.aksjonType shouldBe Aksjon.Type.AVSLUTT
            it.opprettetBehandlingId shouldBe null
            it.utførtAv shouldBe saksbehandler
        }
    }

    @Test
    fun `Behandle en innsending med aksjon av type OpprettKlage`() {
        val testSakId = UUID.randomUUID()

        val slot = slot<KlageMottattHendelse>()
        val klageMediator =
            mockk<KlageMediator>().also {
                every {
                    it.opprettKlage(capture(slot))
                } returns testOppgave
            }
        val innsendingBehandler =
            InnsendingBehandler(
                klageMediator = klageMediator,
                behandlingKlient = mockk(),
            )

        innsendingBehandler.utførAksjon(
            hendelse =
                lagHendelse(
                    aksjon = Aksjon.OpprettKlage(valgtSakId = testSakId),
                    vurdering = "Dette er en vurdering",
                ),
            innsending = testInnsending,
        )
            .let {
                it.innsendingId shouldBe testInnsending.innsendingId
                it.aksjonType shouldBe Aksjon.Type.OPPRETT_KLAGE
                it.opprettetBehandlingId shouldBe testOppgave.behandling.behandlingId
                it.utførtAv shouldBe saksbehandler
            }

        with(slot.captured) {
            ident shouldBe testInnsending.person.ident
            opprettet shouldBe testInnsending.mottatt
            journalpostId shouldBe testInnsending.journalpostId
            sakId shouldBe testSakId
        }
    }

    @Test
    fun `Behandle en innsending med aksjon av type OpprettManuellBehandling`() {
        val saksbehandlerToken = "token"
        val behandlingId = UUID.randomUUID()

        val behandlingKlient =
            mockk<BehandlingKlient>().also {
                every {
                    it.opprettManuellBehandling(
                        personIdent = testInnsending.person.ident,
                        saksbehandlerToken = saksbehandlerToken,
                    )
                } returns Result.success(behandlingId)
            }
        val innsendingBehandler =
            InnsendingBehandler(
                klageMediator = mockk<KlageMediator>(),
                behandlingKlient = behandlingKlient,
            )

        innsendingBehandler.utførAksjon(
            hendelse =
                lagHendelse(
                    aksjon =
                        Aksjon.OpprettManuellBehandling(
                            saksbehandlerToken = saksbehandlerToken,
                            valgtSakId = UUID.randomUUID(),
                        ),
                    vurdering = "Dette er en vurdering",
                ),
            innsending = testInnsending,
        )
            .let {
                it.innsendingId shouldBe testInnsending.innsendingId
                it.aksjonType shouldBe Aksjon.Type.OPPRETT_MANUELL_BEHANDLING
                it.opprettetBehandlingId shouldBe behandlingId
                it.utførtAv shouldBe saksbehandler
            }

        verify(exactly = 1) {
            behandlingKlient.opprettManuellBehandling(testInnsending.person.ident, saksbehandlerToken)
        }
    }

    private fun lagHendelse(
        aksjon: Aksjon,
        vurdering: String,
    ): FerdigstillInnsendingHendelse {
        return FerdigstillInnsendingHendelse(
            innsendingId = testInnsending.innsendingId,
            aksjon = aksjon,
            vurdering = vurdering,
            utførtAv = saksbehandler,
        )
    }
}
