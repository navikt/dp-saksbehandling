package no.nav.dagpenger.saksbehandling.generell

import PersonMediator
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
import no.nav.dagpenger.saksbehandling.db.sak.PostgresSakRepository
import no.nav.dagpenger.saksbehandling.hendelser.FerdigstillGenerellOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.GenerellOppgaveFerdigstiltHendelse
import no.nav.dagpenger.saksbehandling.hendelser.OpprettGenerellOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import org.junit.jupiter.api.Test

class GenerellOppgaveMediatorTest {
    private val testPerson = DBTestHelper.testPerson
    private val saksbehandler =
        Saksbehandler(
            navIdent = "saksbehandler1",
            emptySet(),
        )

    private val personMediatorMock: PersonMediator =
        mockk<PersonMediator>().also {
            every { it.finnEllerOpprettPerson(testPerson.ident) } returns testPerson
        }

    @Test
    fun `Skal opprette generell oppgave, behandling og oppgave ved taImot`() {
        DBTestHelper.withPerson { ds ->
            val generellOppgaveRepository = PostgresGenerellOppgaveRepository(ds)
            val sakRepository = PostgresSakRepository(ds)
            val sakMediator = SakMediator(personMediator = personMediatorMock, sakRepository = sakRepository)
            val oppgaveMediator =
                OppgaveMediator(
                    oppgaveRepository = PostgresOppgaveRepository(ds),
                    behandlingKlient = mockk(),
                    utsendingMediator = mockk(),
                    sakMediator = sakMediator,
                )

            val mediator =
                GenerellOppgaveMediator(
                    generellOppgaveRepository = generellOppgaveRepository,
                    generellOppgaveBehandler = mockk(),
                    sakMediator = sakMediator,
                    oppgaveMediator = oppgaveMediator,
                )

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

            // Verifiser at generell oppgave ble lagret
            val lagretGenerellOppgave = generellOppgaveRepository.hent(generellOppgave.id)
            lagretGenerellOppgave.tittel shouldBe "Meldekort trenger korrigering"

            // Verifiser at oppgave ble opprettet med riktig emneknagg
            val oppgaver = oppgaveMediator.finnOppgaverFor(ident = testPerson.ident)
            oppgaver.size shouldBe 1
            oppgaver.first().behandling.utløstAv shouldBe UtløstAvType.GENERELL
            oppgaver.first().emneknagger shouldBe setOf("MeldekortKorrigering")
        }
    }

    @Test
    fun `Skal ferdigstille generell oppgave med AVSLUTT aksjon`() {
        DBTestHelper.withPerson { ds ->
            val generellOppgaveRepository = PostgresGenerellOppgaveRepository(ds)
            val sakRepository = PostgresSakRepository(ds)
            val sakMediator = SakMediator(personMediator = personMediatorMock, sakRepository = sakRepository)
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
                    sakMediator = sakMediator,
                    oppgaveMediator = oppgaveMediator,
                )

            // Opprett generell oppgave
            val generellOppgave =
                mediator.taImot(
                    OpprettGenerellOppgaveHendelse(
                        ident = testPerson.ident,
                        emneknagg = "TestOppgave",
                        tittel = "Test",
                        beskrivelse = "",
                    ),
                )

            // Tildel oppgaven til saksbehandler
            val oppgaver = oppgaveMediator.finnOppgaverFor(ident = testPerson.ident)
            oppgaveMediator.tildelOppgave(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = oppgaver.first().oppgaveId,
                    ansvarligIdent = saksbehandler.navIdent,
                    utførtAv = saksbehandler,
                ),
            )

            // Ferdigstill
            mediator.ferdigstill(
                FerdigstillGenerellOppgaveHendelse(
                    generellOppgaveId = generellOppgave.id,
                    aksjon = GenerellOppgaveAksjon.Avslutt(null),
                    vurdering = "Alt er OK",
                    utførtAv = saksbehandler,
                ),
            )

            // Verifiser generell oppgave er ferdigstilt
            val ferdigstiltOppgave = generellOppgaveRepository.hent(generellOppgave.id)
            ferdigstiltOppgave.tilstand() shouldBe "FERDIGSTILT"
            ferdigstiltOppgave.vurdering() shouldBe "Alt er OK"
            ferdigstiltOppgave.resultat() shouldBe GenerellOppgave.Resultat.Ingen

            // Verifiser oppgave er ferdigbehandlet
            val oppdatertOppgave = oppgaveMediator.hentOppgave(oppgaver.first().oppgaveId, saksbehandler)
            oppdatertOppgave.tilstand() shouldBe Oppgave.FerdigBehandlet
        }
    }
}
