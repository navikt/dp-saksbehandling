package no.nav.dagpenger.saksbehandling.oppfolging

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.UtløstAvType
import no.nav.dagpenger.saksbehandling.db.DBTestHelper
import no.nav.dagpenger.saksbehandling.db.oppfolging.PostgresOppfølgingRepository
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.db.person.PersonMediator
import no.nav.dagpenger.saksbehandling.db.person.PostgresPersonRepository
import no.nav.dagpenger.saksbehandling.db.sak.PostgresSakRepository
import no.nav.dagpenger.saksbehandling.hendelser.FerdigstillOppfølgingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.OppfølgingFerdigstiltHendelse
import no.nav.dagpenger.saksbehandling.hendelser.OpprettOppfølgingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.sak.SakMediator
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OppfølgingMediatorTest {
    private val testPerson = DBTestHelper.testPerson
    private val saksbehandler = Saksbehandler(navIdent = "saksbehandler1", emptySet())

    @Test
    fun `E2E - opprette og ferdigstille oppfølging`() {
        DBTestHelper.withPerson { ds ->
            val oppfølgingRepository = PostgresOppfølgingRepository(ds)
            val personMediator = PersonMediator(PostgresPersonRepository(ds), mockk())
            val sakMediator = SakMediator(personMediator = personMediator, sakRepository = PostgresSakRepository(ds))
            val oppgaveMediator =
                OppgaveMediator(
                    oppgaveRepository = PostgresOppgaveRepository(ds),
                    behandlingKlient = mockk(),
                    utsendingMediator = mockk(),
                    sakMediator = sakMediator,
                )

            val oppfølgingBehandler =
                mockk<OppfølgingBehandler>().also {
                    every { it.utførAksjon(any(), any(), any()) } answers {
                        val oppgave = firstArg<Oppfølging>()
                        OppfølgingFerdigstiltHendelse(
                            oppfølgingId = oppgave.id,
                            aksjonType = OppfølgingAksjon.Type.AVSLUTT,
                            opprettetBehandlingId = null,
                            utførtAv = saksbehandler,
                        )
                    }
                }

            val mediator =
                OppfølgingMediator(
                    oppfølgingRepository = oppfølgingRepository,
                    oppfølgingBehandler = oppfølgingBehandler,
                    personMediator = personMediator,
                    sakMediator = sakMediator,
                    oppgaveMediator = oppgaveMediator,
                )

            // Opprett
            val resultat =
                mediator.taImot(
                    OpprettOppfølgingHendelse(
                        ident = testPerson.ident,
                        aarsak = "MeldekortKorrigering",
                        tittel = "Meldekort trenger korrigering",
                        beskrivelse = "Se på perioden",
                    ),
                )

            val oppfølging = oppfølgingRepository.hent(resultat.oppfølgingId)
            oppfølging.tilstand() shouldBe "BEHANDLES"

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
                FerdigstillOppfølgingHendelse(
                    oppfølgingId = resultat.oppfølgingId,
                    aksjon = OppfølgingAksjon.Avslutt(null),
                    vurdering = "Alt er OK",
                    utførtAv = saksbehandler,
                ),
            )

            // Verifiser ferdigstilt
            val ferdigstiltOppfølging = oppfølgingRepository.hent(resultat.oppfølgingId)
            ferdigstiltOppfølging.tilstand() shouldBe "FERDIGSTILT"
            ferdigstiltOppfølging.vurdering() shouldBe "Alt er OK"

            val oppdatertOppgave = oppgaveMediator.hentOppgave(oppgaver.first().oppgaveId, saksbehandler)
            oppdatertOppgave.tilstand() shouldBe Oppgave.FerdigBehandlet
        }
    }

    @Test
    fun `oppgave med beholdOppgaven tildeles opprettende saksbehandler`() {
        DBTestHelper.withPerson { ds ->
            val oppfølgingRepository = PostgresOppfølgingRepository(ds)
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
                OppfølgingMediator(
                    oppfølgingRepository = oppfølgingRepository,
                    oppfølgingBehandler = mockk(),
                    personMediator = personMediator,
                    sakMediator = sakMediator,
                    oppgaveMediator = oppgaveMediator,
                )

            val resultat =
                mediator.taImot(
                    OpprettOppfølgingHendelse(
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
            val oppfølgingRepository = PostgresOppfølgingRepository(ds)
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
                OppfølgingMediator(
                    oppfølgingRepository = oppfølgingRepository,
                    oppfølgingBehandler = mockk(),
                    personMediator = personMediator,
                    sakMediator = sakMediator,
                    oppgaveMediator = oppgaveMediator,
                )

            val resultat =
                mediator.taImot(
                    OpprettOppfølgingHendelse(
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
            val oppfølgingRepository = PostgresOppfølgingRepository(ds)
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
                OppfølgingMediator(
                    oppfølgingRepository = oppfølgingRepository,
                    oppfølgingBehandler = mockk(),
                    personMediator = personMediator,
                    sakMediator = sakMediator,
                    oppgaveMediator = oppgaveMediator,
                )

            val frist = LocalDate.now().plusDays(7)

            val resultat =
                mediator.taImot(
                    OpprettOppfølgingHendelse(
                        ident = testPerson.ident,
                        aarsak = "Sykemelding",
                        tittel = "Sjekk sykemelding",
                        beskrivelse = "Kontroller datoer",
                        frist = frist,
                    ),
                )

            // Verifiser Oppfølging har frist
            val oppfølging = oppfølgingRepository.hent(resultat.oppfølgingId)
            oppfølging.frist shouldBe frist

            // Verifiser Oppgave er i PåVent med utsattTil
            val oppgave = oppgaveRepository.hentOppgave(resultat.oppgaveId)
            oppgave.tilstand() shouldBe Oppgave.PåVent
            oppgave.utsattTil() shouldBe frist
        }
    }
}
