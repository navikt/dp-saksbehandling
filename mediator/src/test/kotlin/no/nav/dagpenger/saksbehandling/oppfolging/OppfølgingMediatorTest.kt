package no.nav.dagpenger.saksbehandling.oppfolging

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.HendelseBehandler
import no.nav.dagpenger.saksbehandling.KlageMediator
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.OppgaveMediator
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.TilgangType
import no.nav.dagpenger.saksbehandling.behandling.BehandlingKlient
import no.nav.dagpenger.saksbehandling.db.DBTestHelper
import no.nav.dagpenger.saksbehandling.db.DatabaseSession
import no.nav.dagpenger.saksbehandling.db.Transaksjoner
import no.nav.dagpenger.saksbehandling.db.oppfolging.PostgresOppfølgingRepository
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.db.person.PersonMediator
import no.nav.dagpenger.saksbehandling.db.person.PostgresPersonRepository
import no.nav.dagpenger.saksbehandling.db.sak.PostgresSakRepository
import no.nav.dagpenger.saksbehandling.hendelser.FerdigstillOppfølgingHendelse
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
            val oppfølgingRepository = PostgresOppfølgingRepository(DatabaseSession(lazy { ds }))
            val personMediator = PersonMediator(PostgresPersonRepository(DatabaseSession(lazy { ds })), mockk())
            val sakMediator =
                SakMediator(
                    personMediator = personMediator,
                    sakRepository = PostgresSakRepository(DatabaseSession(lazy { ds })),
                    rapidsConnection = mockk(relaxed = true),
                )
            val oppgaveMediator =
                OppgaveMediator(
                    oppgaveRepository = PostgresOppgaveRepository(DatabaseSession(lazy { ds })),
                    behandlingKlient = mockk(),
                    utsendingMediator = mockk(),
                    sakMediator = sakMediator,
                    rapidsConnection = mockk(relaxed = true),
                )

            val oppfølgingBehandler = mockk<OppfølgingBehandler>()

            val mediator =
                OppfølgingMediator(
                    transaksjoner = Transaksjoner(DatabaseSession(lazy { ds })),
                    oppfølgingRepository = oppfølgingRepository,
                    oppfølgingBehandler = oppfølgingBehandler,
                    personMediator = personMediator,
                    sakMediator = sakMediator,
                    oppgaveMediator = oppgaveMediator,
                )

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

            val oppgaver = oppgaveMediator.finnOppgaverFor(ident = testPerson.ident)
            oppgaver.size shouldBe 1
            oppgaver.first().behandling.utløstAv shouldBe HendelseBehandler.Intern.Oppfølging
            oppgaver.first().emneknagger shouldBe setOf("MeldekortKorrigering")

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

            val ferdigstiltOppfølging = oppfølgingRepository.hent(resultat.oppfølgingId)
            ferdigstiltOppfølging.tilstand() shouldBe "FERDIGSTILT"
            ferdigstiltOppfølging.vurdering() shouldBe "Alt er OK"

            val oppdatertOppgave = oppgaveMediator.hentOppgave(oppgaver.first().oppgaveId, saksbehandler)
            oppdatertOppgave.tilstand() shouldBe Oppgave.FerdigBehandlet
        }
    }

    @Test
    fun `ferdigstill med OpprettOppfølging - frist og beholdOppgaven gir ny oppgave i PåVent`() {
        DBTestHelper.withPerson { ds ->
            val oppfølgingRepository = PostgresOppfølgingRepository(DatabaseSession(lazy { ds }))
            val personMediator = PersonMediator(PostgresPersonRepository(DatabaseSession(lazy { ds })), mockk())
            val sakMediator =
                SakMediator(
                    personMediator = personMediator,
                    sakRepository = PostgresSakRepository(DatabaseSession(lazy { ds })),
                    rapidsConnection = mockk(relaxed = true),
                )
            val oppgaveRepository = PostgresOppgaveRepository(DatabaseSession(lazy { ds }))
            val oppgaveMediator =
                OppgaveMediator(
                    oppgaveRepository = oppgaveRepository,
                    behandlingKlient = mockk(),
                    utsendingMediator = mockk(),
                    sakMediator = sakMediator,
                    rapidsConnection = mockk(relaxed = true),
                )
            val saksbehandler = Saksbehandler("Z999999", emptySet(), setOf(TilgangType.SAKSBEHANDLER))
            val mediator =
                OppfølgingMediator(
                    transaksjoner = Transaksjoner(DatabaseSession(lazy { ds })),
                    oppfølgingRepository = oppfølgingRepository,
                    oppfølgingBehandler = OppfølgingBehandler(mockk<KlageMediator>(), mockk<BehandlingKlient>()),
                    personMediator = personMediator,
                    sakMediator = sakMediator,
                    oppgaveMediator = oppgaveMediator,
                )

            val original =
                mediator.taImot(
                    OpprettOppfølgingHendelse(ident = testPerson.ident, aarsak = "Test", tittel = "Original"),
                )

            oppgaveMediator.tildelOppgave(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = original.oppgaveId,
                    ansvarligIdent = saksbehandler.navIdent,
                    utførtAv = saksbehandler,
                ),
            )

            val frist = LocalDate.now().plusDays(7)

            mediator.ferdigstill(
                FerdigstillOppfølgingHendelse(
                    oppfølgingId = original.oppfølgingId,
                    aksjon =
                        OppfølgingAksjon.OpprettOppfølging(
                            valgtSakId = null,
                            aarsak = "Ny",
                            tittel = "Ny oppfølging",
                            frist = frist,
                            beholdOppgaven = true,
                        ),
                    vurdering = "Trenger ny sjekk",
                    utførtAv = saksbehandler,
                ),
            )

            val oppgaver = oppgaveMediator.finnOppgaverFor(ident = testPerson.ident)
            val nyOppgave = oppgaver.first { it.tilstand() == Oppgave.PåVent }
            nyOppgave.tilstand() shouldBe Oppgave.PåVent
            nyOppgave.utsattTil() shouldBe frist
            nyOppgave.behandlerIdent shouldBe saksbehandler.navIdent
        }
    }

    @Test
    fun `ferdigstill med OpprettOppfølging uten beholdOppgaven gir ny oppgave i KlarTilBehandling uten behandler`() {
        DBTestHelper.withPerson { ds ->
            val oppfølgingRepository = PostgresOppfølgingRepository(DatabaseSession(lazy { ds }))
            val personMediator = PersonMediator(PostgresPersonRepository(DatabaseSession(lazy { ds })), mockk())
            val sakMediator =
                SakMediator(
                    personMediator = personMediator,
                    sakRepository = PostgresSakRepository(DatabaseSession(lazy { ds })),
                    rapidsConnection = mockk(relaxed = true),
                )
            val oppgaveRepository = PostgresOppgaveRepository(DatabaseSession(lazy { ds }))
            val oppgaveMediator =
                OppgaveMediator(
                    oppgaveRepository = oppgaveRepository,
                    behandlingKlient = mockk(),
                    utsendingMediator = mockk(),
                    sakMediator = sakMediator,
                    rapidsConnection = mockk(relaxed = true),
                )
            val saksbehandler = Saksbehandler("Z999999", emptySet(), setOf(TilgangType.SAKSBEHANDLER))
            val mediator =
                OppfølgingMediator(
                    transaksjoner = Transaksjoner(DatabaseSession(lazy { ds })),
                    oppfølgingRepository = oppfølgingRepository,
                    oppfølgingBehandler = OppfølgingBehandler(mockk<KlageMediator>(), mockk<BehandlingKlient>()),
                    personMediator = personMediator,
                    sakMediator = sakMediator,
                    oppgaveMediator = oppgaveMediator,
                )

            val original =
                mediator.taImot(
                    OpprettOppfølgingHendelse(ident = testPerson.ident, aarsak = "Test", tittel = "Original"),
                )

            oppgaveMediator.tildelOppgave(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = original.oppgaveId,
                    ansvarligIdent = saksbehandler.navIdent,
                    utførtAv = saksbehandler,
                ),
            )

            mediator.ferdigstill(
                FerdigstillOppfølgingHendelse(
                    oppfølgingId = original.oppfølgingId,
                    aksjon =
                        OppfølgingAksjon.OpprettOppfølging(
                            valgtSakId = null,
                            aarsak = "Ny",
                            tittel = "Ny oppfølging",
                            frist = null,
                            beholdOppgaven = false,
                        ),
                    vurdering = "Trenger ny sjekk",
                    utførtAv = saksbehandler,
                ),
            )

            val oppgaver = oppgaveMediator.finnOppgaverFor(ident = testPerson.ident)
            val nyOppgave = oppgaver.first { it.tilstand() == Oppgave.KlarTilBehandling }
            nyOppgave.tilstand() shouldBe Oppgave.KlarTilBehandling
            nyOppgave.behandlerIdent shouldBe null
        }
    }

    @Test
    fun `taImot ruller tilbake alle DB-endringer hvis oppgave-lagring feiler`() {
        DBTestHelper.withPerson { ds ->
            val databaseSession = DatabaseSession(lazy { ds })
            val oppfølgingRepository = PostgresOppfølgingRepository(databaseSession)
            val personMediator = PersonMediator(PostgresPersonRepository(databaseSession), mockk())
            val sakMediator =
                SakMediator(
                    personMediator = personMediator,
                    sakRepository = PostgresSakRepository(databaseSession),
                    rapidsConnection = mockk(relaxed = true),
                )
            val oppgaveMediator =
                mockk<OppgaveMediator>().also {
                    every { it.lagOppgaveForOppfølging(any(), any(), any(), any()) } throws
                        RuntimeException("DB-feil ved lagring av oppgave")
                }

            val mediator =
                OppfølgingMediator(
                    transaksjoner = Transaksjoner(databaseSession),
                    oppfølgingRepository = oppfølgingRepository,
                    oppfølgingBehandler = mockk(),
                    personMediator = personMediator,
                    sakMediator = sakMediator,
                    oppgaveMediator = oppgaveMediator,
                )

            val hendelse =
                OpprettOppfølgingHendelse(
                    ident = testPerson.ident,
                    aarsak = "Test",
                    tittel = "Skal rulle tilbake",
                )

            runCatching { mediator.taImot(hendelse) }
                .isFailure shouldBe true

            // Verifiser at oppfølging IKKE ble lagret (rollback)
            oppfølgingRepository.finnForPerson(testPerson.ident) shouldBe emptyList()
        }
    }
}
