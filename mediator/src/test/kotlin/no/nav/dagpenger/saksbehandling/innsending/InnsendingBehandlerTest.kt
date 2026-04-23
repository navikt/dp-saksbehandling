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
import no.nav.dagpenger.saksbehandling.behandling.BehandlingstypeDTO
import no.nav.dagpenger.saksbehandling.hendelser.FerdigstillInnsendingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlageMottattHendelse
import no.nav.dagpenger.saksbehandling.hendelser.OpprettOppfølgingHendelse
import no.nav.dagpenger.saksbehandling.oppfolging.OppfølgingMediator
import no.nav.dagpenger.saksbehandling.oppfolging.OpprettetOppfølging
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class InnsendingBehandlerTest {
    private val saksbehandler = Saksbehandler("Z123456", emptySet())
    private val testInnsending = TestHelper.lagInnsending(vurdering = "Test vurdering")
    private val testOppgave = TestHelper.testOppgave

    @Test
    fun `Behandle en innsending med aksjon av type Avslutt `() {
        val innsendingBehandler =
            InnsendingBehandler(
                klageMediator = mockk(),
                behandlingKlient = mockk(),
                oppfølgingMediator = mockk(),
            )

        innsendingBehandler
            .utførAksjon(
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
                oppfølgingMediator = mockk(),
            )

        innsendingBehandler
            .utførAksjon(
                hendelse =
                    lagHendelse(
                        aksjon = Aksjon.OpprettKlage(valgtSakId = testSakId),
                        vurdering = "Dette er min vurdering",
                    ),
                innsending = testInnsending,
            ).let {
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
                    it.opprettBehandling(
                        personIdent = testInnsending.person.ident,
                        saksbehandlerToken = saksbehandlerToken,
                        behandlingstype = BehandlingstypeDTO.MANUELL,
                        hendelseDato = testInnsending.mottatt.toLocalDate(),
                        hendelseId = testInnsending.innsendingId.toString(),
                        begrunnelse = testInnsending.vurdering()!!,
                    )
                } returns Result.success(behandlingId)
            }
        val innsendingBehandler =
            InnsendingBehandler(
                klageMediator = mockk<KlageMediator>(),
                behandlingKlient = behandlingKlient,
                oppfølgingMediator = mockk(),
            )

        innsendingBehandler
            .utførAksjon(
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
            ).let {
                it.innsendingId shouldBe testInnsending.innsendingId
                it.aksjonType shouldBe Aksjon.Type.OPPRETT_MANUELL_BEHANDLING
                it.opprettetBehandlingId shouldBe behandlingId
                it.utførtAv shouldBe saksbehandler
            }

        verify(exactly = 1) {
            behandlingKlient.opprettBehandling(
                personIdent = testInnsending.person.ident,
                saksbehandlerToken = saksbehandlerToken,
                behandlingstype = BehandlingstypeDTO.MANUELL,
                hendelseDato = testInnsending.mottatt.toLocalDate(),
                hendelseId = testInnsending.innsendingId.toString(),
                begrunnelse = testInnsending.vurdering()!!,
            )
        }
    }

    @Test
    fun `Behandle en innsending med aksjon av type OpprettRevurderingBehandling`() {
        val saksbehandlerToken = "token"
        val behandlingId = UUID.randomUUID()

        val behandlingKlient =
            mockk<BehandlingKlient>().also {
                every {
                    it.opprettBehandling(
                        personIdent = testInnsending.person.ident,
                        saksbehandlerToken = saksbehandlerToken,
                        behandlingstype = BehandlingstypeDTO.REVURDERING,
                        hendelseDato = testInnsending.mottatt.toLocalDate(),
                        hendelseId = testInnsending.innsendingId.toString(),
                        begrunnelse = testInnsending.vurdering()!!,
                    )
                } returns Result.success(behandlingId)
            }
        val innsendingBehandler =
            InnsendingBehandler(
                klageMediator = mockk<KlageMediator>(),
                behandlingKlient = behandlingKlient,
                oppfølgingMediator = mockk(),
            )

        innsendingBehandler
            .utførAksjon(
                hendelse =
                    lagHendelse(
                        aksjon =
                            Aksjon.OpprettRevurderingBehandling(
                                saksbehandlerToken = saksbehandlerToken,
                                valgtSakId = UUID.randomUUID(),
                            ),
                        vurdering = "Dette er en vurdering",
                    ),
                innsending = testInnsending,
            ).let {
                it.innsendingId shouldBe testInnsending.innsendingId
                it.aksjonType shouldBe Aksjon.Type.OPPRETT_REVURDERING_BEHANDLING
                it.opprettetBehandlingId shouldBe behandlingId
                it.utførtAv shouldBe saksbehandler
            }

        verify(exactly = 1) {
            behandlingKlient.opprettBehandling(
                personIdent = testInnsending.person.ident,
                saksbehandlerToken = saksbehandlerToken,
                behandlingstype = BehandlingstypeDTO.REVURDERING,
                hendelseDato = testInnsending.mottatt.toLocalDate(),
                hendelseId = testInnsending.innsendingId.toString(),
                begrunnelse = testInnsending.vurdering()!!,
            )
        }
    }

    @Test
    fun `Behandle en innsending med aksjon av type OpprettOppfølging`() {
        val oppfølgingId = UUID.randomUUID()
        val oppgaveId = UUID.randomUUID()
        val frist = LocalDate.now().plusDays(7)

        val hendelseSlot = slot<OpprettOppfølgingHendelse>()
        val oppfølgingMediator =
            mockk<OppfølgingMediator>().also {
                every { it.taImot(capture(hendelseSlot)) } returns
                    OpprettetOppfølging(
                        oppfølgingId = oppfølgingId,
                        oppgaveId = oppgaveId,
                    )
            }
        val innsendingBehandler =
            InnsendingBehandler(
                klageMediator = mockk(),
                behandlingKlient = mockk(),
                oppfølgingMediator = oppfølgingMediator,
            )

        innsendingBehandler
            .utførAksjon(
                hendelse =
                    lagHendelse(
                        aksjon =
                            Aksjon.OpprettOppfølging(
                                valgtSakId = null,
                                tittel = "Sjekk meldekort",
                                aarsak = "MeldekortKorrigering",
                                frist = frist,
                                beholdOppgaven = true,
                            ),
                        vurdering = "Dette er en vurdering",
                    ),
                innsending = testInnsending,
            ).let {
                it.innsendingId shouldBe testInnsending.innsendingId
                it.aksjonType shouldBe Aksjon.Type.OPPRETT_OPPFOLGING
                it.opprettetBehandlingId shouldBe oppfølgingId
                it.opprettetOppgaveId shouldBe oppgaveId
                it.utførtAv shouldBe saksbehandler
            }

        with(hendelseSlot.captured) {
            ident shouldBe testInnsending.person.ident
            tittel shouldBe "Sjekk meldekort"
            aarsak shouldBe "MeldekortKorrigering"
            this.frist shouldBe frist
            beholdOppgaven shouldBe true
        }

        verify(exactly = 1) { oppfølgingMediator.taImot(any()) }
    }

    private fun lagHendelse(
        aksjon: Aksjon,
        vurdering: String,
    ): FerdigstillInnsendingHendelse =
        FerdigstillInnsendingHendelse(
            innsendingId = testInnsending.innsendingId,
            aksjon = aksjon,
            vurdering = vurdering,
            utførtAv = saksbehandler,
        )
}
