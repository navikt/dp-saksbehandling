package no.nav.dagpenger.saksbehandling

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.behandling.BehandlingKlient
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
import no.nav.dagpenger.saksbehandling.hendelser.OpprettGenerellOppgaveHendelse
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import no.nav.dagpenger.saksbehandling.utsending.UtsendingMediator
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class OppgaveMediatorGenerellOppgaveTest {
    private val oppgaveRepository = mockk<OppgaveRepository>(relaxed = true)
    private val behandlingKlient = mockk<BehandlingKlient>(relaxed = true)
    private val utsendingMediator = mockk<UtsendingMediator>(relaxed = true)
    private val sakMediator = mockk<SakMediator>(relaxed = true)

    private val oppgaveMediator =
        OppgaveMediator(
            oppgaveRepository = oppgaveRepository,
            behandlingKlient = behandlingKlient,
            utsendingMediator = utsendingMediator,
            sakMediator = sakMediator,
        )

    private val testPerson =
        Person(
            id = UUIDv7.ny(),
            ident = "12345678901",
            skjermesSomEgneAnsatte = false,
            adressebeskyttelseGradering = AdressebeskyttelseGradering.UGRADERT,
        )

    @Test
    fun `håndter OpprettGenerellOppgaveHendelse oppretter oppgave med riktig type`() {
        val hendelse =
            OpprettGenerellOppgaveHendelse(
                ident = testPerson.ident,
                emneknagg = "MeldekortKorrigering",
                tittel = "Korrigert meldekort",
                beskrivelse = "Beskrivelse",
            )

        val behandling =
            Behandling(
                behandlingId = UUIDv7.ny(),
                opprettet = LocalDateTime.now(),
                hendelse = hendelse,
                utløstAv = UtløstAvType.GENERELL,
            )

        every { sakMediator.opprettBehandlingForGenerellOppgave(hendelse) } returns Pair(testPerson, behandling)

        oppgaveMediator.håndter(hendelse)

        val oppgaveSlot = slot<Oppgave>()
        verify(exactly = 1) { oppgaveRepository.lagre(capture(oppgaveSlot)) }

        val oppgave = oppgaveSlot.captured
        oppgave.behandling.utløstAv shouldBe UtløstAvType.GENERELL
        oppgave.emneknagger shouldBe setOf("MeldekortKorrigering")
        oppgave.tilstand().type shouldBe Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
        oppgave.meldingOmVedtakKilde() shouldBe Oppgave.MeldingOmVedtakKilde.INGEN

        val dataSlot = slot<GenerellOppgaveData>()
        verify(exactly = 1) { oppgaveRepository.lagreGenerellOppgaveData(capture(dataSlot)) }

        val data = dataSlot.captured
        data.emneknagg shouldBe "MeldekortKorrigering"
        data.tittel shouldBe "Korrigert meldekort"
        data.beskrivelse shouldBe "Beskrivelse"
    }

    @Test
    fun `ferdigstill generell oppgave kaller ikke behandlingKlient`() {
        val hendelse =
            OpprettGenerellOppgaveHendelse(
                ident = testPerson.ident,
                emneknagg = "Test",
                tittel = "Test",
            )

        val behandling =
            Behandling(
                behandlingId = UUIDv7.ny(),
                opprettet = LocalDateTime.now(),
                hendelse = hendelse,
                utløstAv = UtløstAvType.GENERELL,
            )

        val oppgave =
            Oppgave(
                emneknagger = setOf("Test"),
                opprettet = LocalDateTime.now(),
                behandling = behandling,
                person = testPerson,
                meldingOmVedtak =
                    Oppgave.MeldingOmVedtak(
                        kilde = Oppgave.MeldingOmVedtakKilde.INGEN,
                        kontrollertGosysBrev = Oppgave.KontrollertBrev.IKKE_RELEVANT,
                    ),
            ).also { it.settKlarTilBehandling(hendelse) }

        // Simuler at oppgaven er UNDER_BEHANDLING
        oppgave.tildel(
            settOppgaveAnsvarHendelse =
                no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    ansvarligIdent = "Z123456",
                    utførtAv = Saksbehandler("Z123456", emptySet(), emptySet()),
                ),
        )

        every { oppgaveRepository.hentOppgave(oppgave.oppgaveId) } returns oppgave

        oppgaveMediator.ferdigstillOppgave(
            oppgaveId = oppgave.oppgaveId,
            saksbehandler = Saksbehandler("Z123456", emptySet(), emptySet()),
            saksbehandlerToken = "token",
        )

        // Skal IKKE kalle behandlingKlient for GENERELL
        io.mockk.coVerify(exactly = 0) { behandlingKlient.kreverTotrinnskontroll(any(), any()) }
        io.mockk.coVerify(exactly = 0) { behandlingKlient.godkjenn(any(), any(), any()) }
        io.mockk.coVerify(exactly = 0) { behandlingKlient.beslutt(any(), any(), any()) }

        // Men skal lagre oppgaven
        verify(atLeast = 1) { oppgaveRepository.lagre(any()) }
    }
}
