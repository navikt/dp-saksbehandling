package no.nav.dagpenger.saksbehandling.generell

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.UtløstAvType
import no.nav.dagpenger.saksbehandling.db.DBTestHelper
import no.nav.dagpenger.saksbehandling.db.generell.PostgresGenerellOppgaveRepository
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.db.person.PersonMediator
import no.nav.dagpenger.saksbehandling.db.person.PostgresPersonRepository
import no.nav.dagpenger.saksbehandling.db.sak.PostgresSakRepository
import no.nav.dagpenger.saksbehandling.hendelser.FerdigstillGenerellOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.GenerellOppgaveFerdigstiltHendelse
import no.nav.dagpenger.saksbehandling.hendelser.OpprettGenerellOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import org.junit.jupiter.api.Test

class GenerellOppgaveMediatorTest {
    private val testPerson = DBTestHelper.testPerson
    private val saksbehandler = Saksbehandler(navIdent = "saksbehandler1", emptySet())

    @Test
    fun `E2E - opprette og ferdigstille generell oppgave`() {
        DBTestHelper.withPerson { ds ->
            val generellOppgaveRepository = PostgresGenerellOppgaveRepository(ds)
            val personMediator = PersonMediator(PostgresPersonRepository(ds), mockk())
            val sakMediator = SakMediator(personMediator = personMediator, sakRepository = PostgresSakRepository(ds))
            val oppgaveMediator =
                OppgaveMediator(
                    oppgaveRepository = PostgresOppgaveRepository(ds),
                    behandlingKlient = mockk(),
                    utsendingMediator = mockk(),
                    sakMediator = sakMediator,
                )

            val generellOppgaveBehandler =
                mockk<GenerellOppgaveBehandler>().also {
                    every { it.utførAksjon(any(), any()) } answers {
                        val oppgave = firstArg<GenerellOppgave>()
                        GenerellOppgaveFerdigstiltHendelse(
                            generellOppgaveId = oppgave.id,
                            aksjonType = GenerellOppgaveAksjon.Type.AVSLUTT,
                            opprettetBehandlingId = null,
                            utførtAv = saksbehandler,
                        )
                    }
                }

            val mediator =
                GenerellOppgaveMediator(
                    generellOppgaveRepository = generellOppgaveRepository,
                    generellOppgaveBehandler = generellOppgaveBehandler,
                    personMediator = personMediator,
                    sakMediator = sakMediator,
                    oppgaveMediator = oppgaveMediator,
                )

            // Opprett
            val generellOppgave =
                mediator.taImot(
                    OpprettGenerellOppgaveHendelse(
                        ident = testPerson.ident,
                        emneknagg = "MeldekortKorrigering",
                        tittel = "Meldekort trenger korrigering",
                        beskrivelse = "Se på perioden",
                    ),
                )

            generellOppgave.tilstand() shouldBe "BEHANDLES"

            // Verifiser oppgave opprettet med riktig type og emneknagg
            val oppgaver = oppgaveMediator.finnOppgaverFor(ident = testPerson.ident)
            oppgaver.size shouldBe 1
            oppgaver.first().behandling.utløstAv shouldBe UtløstAvType.GENERELL
            oppgaver.first().emneknagger shouldBe setOf("MeldekortKorrigering")

            // Tildel og ferdigstill
            oppgaveMediator.tildelOppgave(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = oppgaver.first().oppgaveId,
                    ansvarligIdent = saksbehandler.navIdent,
                    utførtAv = saksbehandler,
                ),
            )

            mediator.ferdigstill(
                FerdigstillGenerellOppgaveHendelse(
                    generellOppgaveId = generellOppgave.id,
                    aksjon = GenerellOppgaveAksjon.Avslutt(null),
                    vurdering = "Alt er OK",
                    utførtAv = saksbehandler,
                ),
            )

            // Verifiser ferdigstilt
            val ferdigstiltOppgave = generellOppgaveRepository.hent(generellOppgave.id)
            ferdigstiltOppgave.tilstand() shouldBe "FERDIGSTILT"
            ferdigstiltOppgave.vurdering() shouldBe "Alt er OK"

            val oppdatertOppgave = oppgaveMediator.hentOppgave(oppgaver.first().oppgaveId, saksbehandler)
            oppdatertOppgave.tilstand() shouldBe Oppgave.FerdigBehandlet
        }
    }
}
