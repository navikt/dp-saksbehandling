package no.nav.dagpenger.saksbehandling.generell

import PersonMediator
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.Sak
import no.nav.dagpenger.saksbehandling.SakHistorikk
import no.nav.dagpenger.saksbehandling.TestHelper
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.db.generell.GenerellOppgaveRepository
import no.nav.dagpenger.saksbehandling.db.sak.SakRepository
import no.nav.dagpenger.saksbehandling.hendelser.FerdigstillGenerellOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.GenerellOppgaveFerdigstiltHendelse
import no.nav.dagpenger.saksbehandling.hendelser.OpprettGenerellOppgaveHendelse
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import org.junit.jupiter.api.Test

class GenerellOppgaveMediatorTest {
    private val testPerson = TestHelper.testPerson
    private val saksbehandler = TestHelper.saksbehandler

    private val generellOppgaveRepository = mockk<GenerellOppgaveRepository>(relaxed = true)
    private val personMediator = mockk<PersonMediator>()
    private val generellOppgaveBehandler = mockk<GenerellOppgaveBehandler>()
    private val sakMediator = mockk<SakMediator>()
    private val sakRepository = mockk<SakRepository>(relaxed = true)
    private val oppgaveMediator = mockk<OppgaveMediator>(relaxed = true)

    private val mediator =
        GenerellOppgaveMediator(
            generellOppgaveRepository = generellOppgaveRepository,
            personMediator = personMediator,
            generellOppgaveBehandler = generellOppgaveBehandler,
            sakMediator = sakMediator,
            sakRepository = sakRepository,
            oppgaveMediator = oppgaveMediator,
        )

    @Test
    fun `Skal opprette generell oppgave, behandling og oppgave ved taImot`() {
        every { personMediator.finnEllerOpprettPerson(testPerson.ident) } returns testPerson

        val hendelse =
            OpprettGenerellOppgaveHendelse(
                ident = testPerson.ident,
                emneknagg = "MeldekortKorrigering",
                tittel = "Meldekort trenger korrigering",
                beskrivelse = "Se på perioden",
            )

        val generellOppgave = mediator.taImot(hendelse)

        generellOppgave.tittel shouldBe "Meldekort trenger korrigering"
        generellOppgave.beskrivelse shouldBe "Se på perioden"
        generellOppgave.tilstand() shouldBe "BEHANDLES"
        generellOppgave.person shouldBe testPerson

        val generellOppgaveSlot = slot<GenerellOppgave>()
        verify { generellOppgaveRepository.lagre(capture(generellOppgaveSlot)) }
        generellOppgaveSlot.captured.id shouldBe generellOppgave.id

        val behandlingSlot = slot<Behandling>()
        verify { sakRepository.lagreBehandling(testPerson.id, null, capture(behandlingSlot)) }
        behandlingSlot.captured.behandlingId shouldBe generellOppgave.id

        verify { oppgaveMediator.lagOppgaveForGenerellOppgave(hendelse, any(), testPerson) }
    }

    @Test
    fun `Skal ferdigstille generell oppgave med AVSLUTT aksjon`() {
        val generellOppgave =
            TestHelper.lagGenerellOppgave(
                person = testPerson,
                tittel = "Test",
            )

        every { generellOppgaveRepository.hent(generellOppgave.id) } returns generellOppgave
        every { generellOppgaveBehandler.utførAksjon(any(), any()) } returns
            GenerellOppgaveFerdigstiltHendelse(
                generellOppgaveId = generellOppgave.id,
                aksjonType = GenerellOppgaveAksjon.Type.AVSLUTT,
                opprettetBehandlingId = null,
                utførtAv = saksbehandler,
            )

        val hendelse =
            FerdigstillGenerellOppgaveHendelse(
                generellOppgaveId = generellOppgave.id,
                aksjon = GenerellOppgaveAksjon.Avslutt(null),
                vurdering = "Alt er OK",
                utførtAv = saksbehandler,
            )

        mediator.ferdigstill(hendelse)

        generellOppgave.tilstand() shouldBe "FERDIGSTILT"
        generellOppgave.vurdering() shouldBe "Alt er OK"
        generellOppgave.resultat() shouldBe GenerellOppgave.Resultat.Ingen

        verify(exactly = 2) { generellOppgaveRepository.lagre(generellOppgave) }
        verify { oppgaveMediator.ferdigstillOppgave(any<GenerellOppgaveFerdigstiltHendelse>()) }
    }

    @Test
    fun `Skal ferdigstille generell oppgave med OPPRETT_KLAGE aksjon`() {
        val generellOppgave =
            TestHelper.lagGenerellOppgave(
                person = testPerson,
                tittel = "Klagesak",
            )
        val sakId = UUIDv7.ny()
        val opprettetBehandlingId = UUIDv7.ny()

        every { generellOppgaveRepository.hent(generellOppgave.id) } returns generellOppgave
        every { generellOppgaveBehandler.utførAksjon(any(), any()) } returns
            GenerellOppgaveFerdigstiltHendelse(
                generellOppgaveId = generellOppgave.id,
                aksjonType = GenerellOppgaveAksjon.Type.OPPRETT_KLAGE,
                opprettetBehandlingId = opprettetBehandlingId,
                utførtAv = saksbehandler,
            )

        val hendelse =
            FerdigstillGenerellOppgaveHendelse(
                generellOppgaveId = generellOppgave.id,
                aksjon = GenerellOppgaveAksjon.OpprettKlage(sakId),
                vurdering = "Klage opprettet",
                utførtAv = saksbehandler,
            )

        mediator.ferdigstill(hendelse)

        generellOppgave.tilstand() shouldBe "FERDIGSTILT"
        val resultat = generellOppgave.resultat() as GenerellOppgave.Resultat.Klage
        resultat.behandlingId shouldBe opprettetBehandlingId
    }

    @Test
    fun `Skal ferdigstille generell oppgave med OPPRETT_MANUELL_BEHANDLING aksjon`() {
        val generellOppgave =
            TestHelper.lagGenerellOppgave(
                person = testPerson,
                tittel = "Manuell behandling",
            )
        val sakId = UUIDv7.ny()
        val opprettetBehandlingId = UUIDv7.ny()

        every { generellOppgaveRepository.hent(generellOppgave.id) } returns generellOppgave
        every { generellOppgaveBehandler.utførAksjon(any(), any()) } returns
            GenerellOppgaveFerdigstiltHendelse(
                generellOppgaveId = generellOppgave.id,
                aksjonType = GenerellOppgaveAksjon.Type.OPPRETT_MANUELL_BEHANDLING,
                opprettetBehandlingId = opprettetBehandlingId,
                utførtAv = saksbehandler,
            )

        val hendelse =
            FerdigstillGenerellOppgaveHendelse(
                generellOppgaveId = generellOppgave.id,
                aksjon =
                    GenerellOppgaveAksjon.OpprettManuellBehandling(
                        saksbehandlerToken = "token",
                        valgtSakId = sakId,
                    ),
                vurdering = "Manuell behandling opprettet",
                utførtAv = saksbehandler,
            )

        mediator.ferdigstill(hendelse)

        generellOppgave.tilstand() shouldBe "FERDIGSTILT"
        generellOppgave.valgtSakId() shouldBe sakId
        val resultat = generellOppgave.resultat() as GenerellOppgave.Resultat.RettTilDagpenger
        resultat.behandlingId shouldBe opprettetBehandlingId
    }

    @Test
    fun `Skal hente generell oppgave`() {
        val generellOppgave =
            TestHelper.lagGenerellOppgave(
                person = testPerson,
                tittel = "Hentet oppgave",
            )

        every { generellOppgaveRepository.hent(generellOppgave.id) } returns generellOppgave

        val hentet = mediator.hent(generellOppgave.id, saksbehandler)

        hentet.id shouldBe generellOppgave.id
        hentet.tittel shouldBe "Hentet oppgave"
    }

    @Test
    fun `Skal hente lovlige saker`() {
        val sak =
            Sak(
                sakId = UUIDv7.ny(),
                søknadId = UUIDv7.ny(),
                opprettet = TestHelper.opprettetNå,
            )
        val sakHistorikk = SakHistorikk.rehydrer(testPerson, setOf(sak))

        every { sakMediator.finnSakHistorikk(testPerson.ident) } returns sakHistorikk

        val lovligeSaker = mediator.hentLovligeSaker(testPerson.ident)

        lovligeSaker.size shouldBe 1
        lovligeSaker.first().sakId shouldBe sak.sakId
    }

    @Test
    fun `Skal returnere tom liste når ingen sakhistorikk finnes`() {
        every { sakMediator.finnSakHistorikk(testPerson.ident) } returns null

        val lovligeSaker = mediator.hentLovligeSaker(testPerson.ident)

        lovligeSaker shouldBe emptyList()
    }
}
