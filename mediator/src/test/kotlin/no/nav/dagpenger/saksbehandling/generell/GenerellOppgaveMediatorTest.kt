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
import java.time.LocalDate

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
                    every { it.utførAksjon(any(), any(), any()) } answers {
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
            val resultat =
                mediator.taImot(
                    OpprettGenerellOppgaveHendelse(
                        ident = testPerson.ident,
                        aarsak = "MeldekortKorrigering",
                        tittel = "Meldekort trenger korrigering",
                        beskrivelse = "Se på perioden",
                    ),
                )

            val generellOppgave = generellOppgaveRepository.hent(resultat.generellOppgaveId)
            generellOppgave.tilstand() shouldBe "BEHANDLES"

            // Verifiser oppgave opprettet med riktig type og årsak
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
                    generellOppgaveId = resultat.generellOppgaveId,
                    aksjon = GenerellOppgaveAksjon.Avslutt(null),
                    vurdering = "Alt er OK",
                    utførtAv = saksbehandler,
                ),
            )

            // Verifiser ferdigstilt
            val ferdigstiltGenerellOppgave = generellOppgaveRepository.hent(resultat.generellOppgaveId)
            ferdigstiltGenerellOppgave.tilstand() shouldBe "FERDIGSTILT"
            ferdigstiltGenerellOppgave.vurdering() shouldBe "Alt er OK"

            val oppdatertOppgave = oppgaveMediator.hentOppgave(oppgaver.first().oppgaveId, saksbehandler)
            oppdatertOppgave.tilstand() shouldBe Oppgave.FerdigBehandlet
        }
    }

    @Test
    fun `oppgave med beholdOppgaven tildeles opprettende saksbehandler`() {
        DBTestHelper.withPerson { ds ->
            val generellOppgaveRepository = PostgresGenerellOppgaveRepository(ds)
            val personMediator = PersonMediator(PostgresPersonRepository(ds), mockk())
            val sakMediator = SakMediator(personMediator = personMediator, sakRepository = PostgresSakRepository(ds))
            val oppgaveRepository = PostgresOppgaveRepository(ds)
            val oppgaveMediator =
                OppgaveMediator(
                    oppgaveRepository = oppgaveRepository,
                    behandlingKlient = mockk(),
                    utsendingMediator = mockk(),
                    sakMediator = sakMediator,
                )

            val mediator =
                GenerellOppgaveMediator(
                    generellOppgaveRepository = generellOppgaveRepository,
                    generellOppgaveBehandler = mockk(),
                    personMediator = personMediator,
                    sakMediator = sakMediator,
                    oppgaveMediator = oppgaveMediator,
                )

            val resultat =
                mediator.taImot(
                    OpprettGenerellOppgaveHendelse(
                        ident = testPerson.ident,
                        aarsak = "Sykemelding",
                        tittel = "Tildel til meg",
                        beholdOppgaven = true,
                        utførtAv = saksbehandler,
                    ),
                )

            val oppgave = oppgaveRepository.hentOppgave(resultat.oppgaveId)
            oppgave.behandlerIdent shouldBe saksbehandler.navIdent
        }
    }

    @Test
    fun `oppgave uten beholdOppgaven tildeles ikke saksbehandler`() {
        DBTestHelper.withPerson { ds ->
            val generellOppgaveRepository = PostgresGenerellOppgaveRepository(ds)
            val personMediator = PersonMediator(PostgresPersonRepository(ds), mockk())
            val sakMediator = SakMediator(personMediator = personMediator, sakRepository = PostgresSakRepository(ds))
            val oppgaveRepository = PostgresOppgaveRepository(ds)
            val oppgaveMediator =
                OppgaveMediator(
                    oppgaveRepository = oppgaveRepository,
                    behandlingKlient = mockk(),
                    utsendingMediator = mockk(),
                    sakMediator = sakMediator,
                )

            val mediator =
                GenerellOppgaveMediator(
                    generellOppgaveRepository = generellOppgaveRepository,
                    generellOppgaveBehandler = mockk(),
                    personMediator = personMediator,
                    sakMediator = sakMediator,
                    oppgaveMediator = oppgaveMediator,
                )

            val resultat =
                mediator.taImot(
                    OpprettGenerellOppgaveHendelse(
                        ident = testPerson.ident,
                        aarsak = "Sykemelding",
                        tittel = "Ikke tildel til meg",
                        beholdOppgaven = false,
                        utførtAv = saksbehandler,
                    ),
                )

            val oppgave = oppgaveRepository.hentOppgave(resultat.oppgaveId)
            oppgave.behandlerIdent shouldBe null
        }
    }

    @Test
    fun `oppgave med frist opprettes i PåVent tilstand`() {
        DBTestHelper.withPerson { ds ->
            val generellOppgaveRepository = PostgresGenerellOppgaveRepository(ds)
            val personMediator = PersonMediator(PostgresPersonRepository(ds), mockk())
            val sakMediator = SakMediator(personMediator = personMediator, sakRepository = PostgresSakRepository(ds))
            val oppgaveRepository = PostgresOppgaveRepository(ds)
            val oppgaveMediator =
                OppgaveMediator(
                    oppgaveRepository = oppgaveRepository,
                    behandlingKlient = mockk(),
                    utsendingMediator = mockk(),
                    sakMediator = sakMediator,
                )

            val mediator =
                GenerellOppgaveMediator(
                    generellOppgaveRepository = generellOppgaveRepository,
                    generellOppgaveBehandler = mockk(),
                    personMediator = personMediator,
                    sakMediator = sakMediator,
                    oppgaveMediator = oppgaveMediator,
                )

            val frist = LocalDate.now().plusDays(7)

            val resultat =
                mediator.taImot(
                    OpprettGenerellOppgaveHendelse(
                        ident = testPerson.ident,
                        aarsak = "Sykemelding",
                        tittel = "Sjekk sykemelding",
                        beskrivelse = "Kontroller datoer",
                        frist = frist,
                    ),
                )

            // Verifiser GenerellOppgave har frist
            val generellOppgave = generellOppgaveRepository.hent(resultat.generellOppgaveId)
            generellOppgave.frist shouldBe frist

            // Verifiser Oppgave er i PåVent med utsattTil
            val oppgave = oppgaveRepository.hentOppgave(resultat.oppgaveId)
            oppgave.tilstand() shouldBe Oppgave.PåVent
            oppgave.utsattTil() shouldBe frist
        }
    }
}
