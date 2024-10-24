package no.nav.dagpenger.saksbehandling.db

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.FORTROLIG
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.STRENGT_FORTROLIG
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Oppgave.FerdigBehandlet
import no.nav.dagpenger.saksbehandling.Oppgave.KlarTilBehandling
import no.nav.dagpenger.saksbehandling.Oppgave.Opprettet
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_KONTROLL
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_KONTROLL
import no.nav.dagpenger.saksbehandling.Oppgave.UnderBehandling
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.TilgangType
import no.nav.dagpenger.saksbehandling.Tilstandsendring
import no.nav.dagpenger.saksbehandling.Tilstandslogg
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.db.oppgave.DataNotFoundException
import no.nav.dagpenger.saksbehandling.db.oppgave.Periode
import no.nav.dagpenger.saksbehandling.db.oppgave.Periode.Companion.UBEGRENSET_PERIODE
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.db.oppgave.Søkefilter
import no.nav.dagpenger.saksbehandling.db.oppgave.TildelNesteOppgaveFilter
import no.nav.dagpenger.saksbehandling.hendelser.GodkjentBehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlarTilKontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.NesteOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ToTrinnskontrollHendelse
import no.nav.dagpenger.saksbehandling.lagBehandling
import no.nav.dagpenger.saksbehandling.lagOppgave
import no.nav.dagpenger.saksbehandling.lagPerson
import no.nav.dagpenger.saksbehandling.opprettetNå
import no.nav.dagpenger.saksbehandling.testPerson
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class PostgresOppgaveRepositoryTest {
    private val saksbehandler =
        Saksbehandler("saksbehandler", setOf("SaksbehandlerADGruppe"), setOf(TilgangType.SAKSBEHANDLER))
    private val beslutter =
        Saksbehandler(
            "beslutter",
            setOf("SaksbehandlerADGruppe", "BeslutterADGruppe"),
            setOf(TilgangType.BESLUTTER, TilgangType.SAKSBEHANDLER),
        )
    private val oppgaveIdTest = UUIDv7.ny()

    @Test
    fun `Skal kunne lagre og hente person`() {
        withMigratedDb { ds ->
            val repo = PostgresOppgaveRepository(ds)
            repo.lagre(testPerson)

            val personFraDatabase = repo.finnPerson(testPerson.ident)
            personFraDatabase shouldBe testPerson
        }
    }

    @Test
    fun `Skal kunne oppdatere egen ansatt skjerming på en person`() {
        withMigratedDb { ds ->
            val repo = PostgresOppgaveRepository(ds)
            repo.lagre(testPerson)
            repo.finnPerson(testPerson.ident) shouldBe testPerson

            val oppdatertPerson = testPerson.copy(skjermesSomEgneAnsatte = true)
            repo.lagre(oppdatertPerson)
            repo.finnPerson(oppdatertPerson.ident) shouldBe oppdatertPerson
        }
    }

    @Test
    fun `Skal kunne oppdatere bare egen ansatt skjerming på en person`() {
        withMigratedDb { ds ->
            val repo = PostgresOppgaveRepository(ds)
            repo.lagre(testPerson)
            repo.hentPerson(testPerson.ident).skjermesSomEgneAnsatte shouldBe false

            repo.oppdaterSkjermingStatus(testPerson.ident, true)
            repo.hentPerson(testPerson.ident).skjermesSomEgneAnsatte shouldBe true
        }
    }

    @Test
    fun `Skal kunne oppdatere adresse beskyttet status på en person`() {
        withMigratedDb { ds ->
            val repo = PostgresOppgaveRepository(ds)
            repo.lagre(testPerson)
            repo.hentPerson(testPerson.ident).adressebeskyttelseGradering shouldBe UGRADERT

            repo.oppdaterAdressebeskyttetStatus(testPerson.ident, STRENGT_FORTROLIG)
            repo.hentPerson(testPerson.ident).adressebeskyttelseGradering shouldBe STRENGT_FORTROLIG
        }
    }

    @Test
    fun `Det finnes ikke flere ledige oppgaver`() {
        withMigratedDb { ds ->
            val repo = PostgresOppgaveRepository(ds)
            val saksbehandler =
                Saksbehandler(
                    navIdent = "NAVIdent2",
                    grupper = emptySet(),
                )
            repo.lagre(lagOppgave(tilstand = FerdigBehandlet))
            repo.tildelOgHentNesteOppgave(
                nesteOppgaveHendelse =
                    NesteOppgaveHendelse(
                        ansvarligIdent = saksbehandler.navIdent,
                        utførtAv = saksbehandler,
                    ),
                filter =
                    TildelNesteOppgaveFilter(
                        periode = UBEGRENSET_PERIODE,
                        emneknagg = emptySet(),
                        harTilgangTilAdressebeskyttelser = setOf(FORTROLIG),
                    ),
            ) shouldBe null
        }
    }

    @Test
    fun `Finn neste ledige oppgave som ikke gjelder egne ansatte`() {
        withMigratedDb { ds ->
            val repo = PostgresOppgaveRepository(ds)

            val eldsteOppgaveMedSkjermingAvEgenAnsatt =
                lagOppgave(
                    tilstand = KlarTilBehandling,
                    opprettet = opprettetNå.minusDays(5),
                    person =
                        Person(
                            ident = "12345123451",
                            skjermesSomEgneAnsatte = true,
                            adressebeskyttelseGradering = UGRADERT,
                        ),
                )
            repo.lagre(eldsteOppgaveMedSkjermingAvEgenAnsatt)

            val eldsteOppgaveUtenSkjermingAvEgenAnsatt =
                lagOppgave(
                    tilstand = KlarTilBehandling,
                    opprettet = opprettetNå.minusDays(1),
                    person =
                        Person(
                            ident = "11111222222",
                            skjermesSomEgneAnsatte = false,
                            adressebeskyttelseGradering = UGRADERT,
                        ),
                )
            repo.lagre(eldsteOppgaveUtenSkjermingAvEgenAnsatt)

            val nyesteOppgaveUtenSkjermingAvEgenAnsatt =
                lagOppgave(
                    tilstand = KlarTilBehandling,
                    opprettet = opprettetNå,
                    person =
                        Person(
                            ident = "11111333333",
                            skjermesSomEgneAnsatte = false,
                            adressebeskyttelseGradering = UGRADERT,
                        ),
                )
            repo.lagre(nyesteOppgaveUtenSkjermingAvEgenAnsatt)

            val saksbehandlerUtenTilgangTilEgneAnsatte =
                Saksbehandler(
                    navIdent = "NAVIdent2",
                    grupper = emptySet(),
                )
            val nesteOppgave =
                repo.tildelOgHentNesteOppgave(
                    nesteOppgaveHendelse =
                        NesteOppgaveHendelse(
                            ansvarligIdent = saksbehandlerUtenTilgangTilEgneAnsatte.navIdent,
                            utførtAv = saksbehandlerUtenTilgangTilEgneAnsatte,
                        ),
                    filter =
                        TildelNesteOppgaveFilter(
                            periode = UBEGRENSET_PERIODE,
                            emneknagg = emptySet(),
                            harTilgangTilEgneAnsatte = false,
                            harTilgangTilAdressebeskyttelser = setOf(UGRADERT),
                        ),
                )!!
            nesteOppgave.oppgaveId shouldBe eldsteOppgaveUtenSkjermingAvEgenAnsatt.oppgaveId
            nesteOppgave.behandlerIdent shouldBe saksbehandlerUtenTilgangTilEgneAnsatte.navIdent
            nesteOppgave.tilstand().type shouldBe UNDER_BEHANDLING

            val saksbehandlerMedTilgangTilEgneAnsatte =
                Saksbehandler(
                    navIdent = "NAVIdent3",
                    grupper = emptySet(),
                )
            val nesteOppgaveMedTilgang =
                repo.tildelOgHentNesteOppgave(
                    nesteOppgaveHendelse =
                        NesteOppgaveHendelse(
                            ansvarligIdent = saksbehandlerMedTilgangTilEgneAnsatte.navIdent,
                            utførtAv = saksbehandlerMedTilgangTilEgneAnsatte,
                        ),
                    filter =
                        TildelNesteOppgaveFilter(
                            periode = UBEGRENSET_PERIODE,
                            emneknagg = emptySet(),
                            harTilgangTilEgneAnsatte = true,
                            harTilgangTilAdressebeskyttelser = setOf(UGRADERT),
                        ),
                )!!

            nesteOppgaveMedTilgang.oppgaveId shouldBe eldsteOppgaveMedSkjermingAvEgenAnsatt.oppgaveId
            nesteOppgaveMedTilgang.behandlerIdent shouldBe saksbehandlerMedTilgangTilEgneAnsatte.navIdent
            nesteOppgaveMedTilgang.tilstand().type shouldBe UNDER_BEHANDLING
        }
    }

    @Test
    fun `Finn neste ledige oppgave som ikke gjelder adressebeskyttede personer`() {
        withMigratedDb { ds ->
            val repo = PostgresOppgaveRepository(ds)

            val eldsteOppgaveMedAdressebeskyttelse =
                lagOppgave(
                    tilstand = KlarTilBehandling,
                    opprettet = opprettetNå.minusDays(5),
                    person =
                        Person(
                            ident = "12345123451",
                            skjermesSomEgneAnsatte = false,
                            adressebeskyttelseGradering = FORTROLIG,
                        ),
                )
            repo.lagre(eldsteOppgaveMedAdressebeskyttelse)

            val eldsteOppgaveUtenAdressebeskyttelse =
                lagOppgave(
                    tilstand = KlarTilBehandling,
                    opprettet = opprettetNå.minusDays(1),
                    person =
                        Person(
                            ident = "11111222222",
                            skjermesSomEgneAnsatte = false,
                            adressebeskyttelseGradering = UGRADERT,
                        ),
                )
            repo.lagre(eldsteOppgaveUtenAdressebeskyttelse)

            val saksbehandlernUtenTilgangTilAdressebeskyttede =
                Saksbehandler(
                    navIdent = "NAVIdent2",
                    grupper = emptySet(),
                )
            val nesteOppgave =
                repo.tildelOgHentNesteOppgave(
                    nesteOppgaveHendelse =
                        NesteOppgaveHendelse(
                            ansvarligIdent = saksbehandlernUtenTilgangTilAdressebeskyttede.navIdent,
                            utførtAv = saksbehandlernUtenTilgangTilAdressebeskyttede,
                        ),
                    filter =
                        TildelNesteOppgaveFilter(
                            periode = UBEGRENSET_PERIODE,
                            emneknagg = emptySet(),
                            harTilgangTilEgneAnsatte = false,
                            harTilgangTilAdressebeskyttelser = setOf(UGRADERT),
                        ),
                )
            requireNotNull(nesteOppgave)
            nesteOppgave.oppgaveId shouldBe eldsteOppgaveUtenAdressebeskyttelse.oppgaveId
            nesteOppgave.behandlerIdent shouldBe saksbehandlernUtenTilgangTilAdressebeskyttede.navIdent
            nesteOppgave.tilstand().type shouldBe UNDER_BEHANDLING

            val saksbehandlerMedTilgangTilEgneAnsatte =
                Saksbehandler(
                    navIdent = "NAVIdent3",
                    grupper = emptySet(),
                )
            val nesteOppgaveMedTilgang =
                repo.tildelOgHentNesteOppgave(
                    nesteOppgaveHendelse =
                        NesteOppgaveHendelse(
                            ansvarligIdent = saksbehandlerMedTilgangTilEgneAnsatte.navIdent,
                            utførtAv = saksbehandlerMedTilgangTilEgneAnsatte,
                        ),
                    filter =
                        TildelNesteOppgaveFilter(
                            periode = UBEGRENSET_PERIODE,
                            emneknagg = emptySet(),
                            harTilgangTilEgneAnsatte = true,
                            harTilgangTilAdressebeskyttelser = setOf(UGRADERT, FORTROLIG),
                        ),
                )!!

            nesteOppgaveMedTilgang.oppgaveId shouldBe eldsteOppgaveMedAdressebeskyttelse.oppgaveId
            nesteOppgaveMedTilgang.behandlerIdent shouldBe saksbehandlerMedTilgangTilEgneAnsatte.navIdent
            nesteOppgaveMedTilgang.tilstand().type shouldBe UNDER_BEHANDLING
        }
    }

    @Test
    fun `Ved tildeling av neste oppgave, skal det lagres en tilstandsendring i tilstandsendringsloggen`() {
        withMigratedDb { ds ->

            val saksbehandler =
                Saksbehandler(
                    navIdent = "NAVIdent",
                    grupper = emptySet(),
                )
            val repo = PostgresOppgaveRepository(ds)
            val nesteOppgaveHendelse =
                NesteOppgaveHendelse(
                    ansvarligIdent = saksbehandler.navIdent,
                    utførtAv = saksbehandler,
                )
            val oppgave =
                lagOppgave(
                    tilstand = KlarTilBehandling,
                    opprettet = opprettetNå,
                    emneknagger = setOf("Testknagg"),
                )
            repo.lagre(oppgave)
            val rehydrertOppgave = repo.hentOppgave(oppgave.oppgaveId)

            val antallTilstandsendringer = rehydrertOppgave.tilstandslogg.size

            val filter =
                TildelNesteOppgaveFilter(
                    periode = UBEGRENSET_PERIODE,
                    emneknagg = setOf("Testknagg"),
                    harTilgangTilAdressebeskyttelser = setOf(UGRADERT),
                )
            val nesteOppgave = repo.tildelOgHentNesteOppgave(nesteOppgaveHendelse, filter)
            nesteOppgave!!.tilstandslogg.size shouldBe antallTilstandsendringer + 1
        }
    }

    @Test
    fun `Ved tildeling av neste oppgave, skal man ut fra filteret finne eldste ledige oppgave klar til behandling og oppdatere den`() {
        withMigratedDb { ds ->

            val testSaksbehandler =
                Saksbehandler(
                    navIdent = "NAVIdent",
                    grupper = emptySet(),
                )
            val repo = PostgresOppgaveRepository(ds)

            val yngsteLedigeOppgave =
                lagOppgave(
                    tilstand = KlarTilBehandling,
                    opprettet = opprettetNå,
                )

            val nestEldsteLedigeOppgave =
                lagOppgave(
                    tilstand = KlarTilBehandling,
                    opprettet = opprettetNå.minusDays(5),
                    emneknagger = setOf("Testknagg"),
                )

            val eldsteLedigeOppgave =
                lagOppgave(
                    tilstand = KlarTilBehandling,
                    opprettet = opprettetNå.minusDays(10),
                )

            val endaEldreTildeltOppgave =
                lagOppgave(
                    tilstand = KlarTilBehandling,
                    opprettet = opprettetNå.minusDays(11),
                    saksbehandlerIdent = saksbehandler.navIdent,
                )

            val endaEldreFerdigOppgave =
                lagOppgave(
                    tilstand = FerdigBehandlet,
                    opprettet = opprettetNå.minusDays(12),
                    saksbehandlerIdent = testSaksbehandler.navIdent,
                )

            val endaEldreOpprettetOppgave =
                lagOppgave(
                    tilstand = Opprettet,
                    opprettet = opprettetNå.minusDays(13),
                )

            repo.lagre(yngsteLedigeOppgave)
            repo.lagre(nestEldsteLedigeOppgave)
            repo.lagre(eldsteLedigeOppgave)
            repo.lagre(endaEldreTildeltOppgave)
            repo.lagre(endaEldreFerdigOppgave)
            repo.lagre(endaEldreOpprettetOppgave)

            val filter =
                TildelNesteOppgaveFilter(
                    periode = UBEGRENSET_PERIODE,
                    emneknagg = setOf("Testknagg"),
                    harTilgangTilAdressebeskyttelser = setOf(UGRADERT),
                )
            val nesteOppgave =
                repo.tildelOgHentNesteOppgave(
                    nesteOppgaveHendelse =
                        NesteOppgaveHendelse(
                            ansvarligIdent = testSaksbehandler.navIdent,
                            utførtAv = testSaksbehandler,
                        ),
                    filter = filter,
                )
            nesteOppgave!!.oppgaveId shouldBe nestEldsteLedigeOppgave.oppgaveId
            nesteOppgave.behandlerIdent shouldBe testSaksbehandler.navIdent
            nesteOppgave.tilstand().type shouldBe UNDER_BEHANDLING

            val filter2 =
                TildelNesteOppgaveFilter(
                    periode = Periode(fom = opprettetNå.toLocalDate(), tom = opprettetNå.toLocalDate()),
                    emneknagg = emptySet(),
                    harTilgangTilAdressebeskyttelser = setOf(UGRADERT),
                )
            val nesteOppgave2 =
                repo.tildelOgHentNesteOppgave(
                    nesteOppgaveHendelse =
                        NesteOppgaveHendelse(
                            ansvarligIdent = testSaksbehandler.navIdent,
                            utførtAv = testSaksbehandler,
                        ),
                    filter = filter2,
                )
            nesteOppgave2!!.oppgaveId shouldBe yngsteLedigeOppgave.oppgaveId
        }
    }

    @Test
    fun `Skal kunne slette behandling`() {
        val testOppgave = lagOppgave(emneknagger = setOf("hugga", "bugga"))
        withMigratedDb { ds ->
            val repo = PostgresOppgaveRepository(ds)
            repo.lagre(testOppgave)
            repo.slettBehandling(testOppgave.behandlingId)

            assertThrows<DataNotFoundException> {
                repo.hentBehandling(testOppgave.behandlingId)
            }

            assertThrows<DataNotFoundException> {
                repo.hentPerson(testPerson.ident)
            }

            assertThrows<DataNotFoundException> {
                repo.hentOppgave(oppgaveIdTest)
            }

            sessionOf(ds).use { session ->
                session.run(
                    queryOf(
                        //language=PostgreSQL
                        statement = """SELECT COUNT(*) FROM emneknagg_v1 WHERE oppgave_id = '${testOppgave.oppgaveId}'""",
                    ).map { row ->
                        row.int(1)
                    }.asSingle,
                )
            } shouldBe 0
        }
    }

    @Test
    fun `Exception hvis vi ikke får hentet person basert på ident`() {
        withMigratedDb { ds ->
            val repo = PostgresOppgaveRepository(ds)

            shouldThrow<DataNotFoundException> {
                repo.hentPerson(testPerson.ident)
            }
        }
    }

    @Test
    fun `Exception hvis vi ikke får hentet behandling basert på behandlingId`() {
        withMigratedDb { ds ->
            val repo = PostgresOppgaveRepository(ds)

            shouldThrow<DataNotFoundException> {
                repo.hentBehandling(UUIDv7.ny())
            }
        }
    }

    @Test
    fun `Skal kunne lagre en behandling`() {
        val testBehandling = lagBehandling()
        val behandlingMedSøknadsbehandlingOpprettetHendelse =
            lagBehandling(
                hendelse =
                    SøknadsbehandlingOpprettetHendelse(
                        søknadId = UUIDv7.ny(),
                        behandlingId = UUIDv7.ny(),
                        ident = testPerson.ident,
                        opprettet = LocalDateTime.now(),
                    ),
            )

        withMigratedDb { ds ->
            val repo = PostgresOppgaveRepository(ds)
            repo.lagre(testBehandling)
            val behandlingFraDatabase = repo.hentBehandling(testBehandling.behandlingId)
            behandlingFraDatabase shouldBe testBehandling

            repo.lagre(behandlingMedSøknadsbehandlingOpprettetHendelse)
            repo.hentBehandling(
                behandlingMedSøknadsbehandlingOpprettetHendelse.behandlingId,
            ) shouldBe behandlingMedSøknadsbehandlingOpprettetHendelse
        }
    }

    @Test
    fun `Skal kunne lagre en oppgave flere ganger`() {
        val testOppgave = lagOppgave()
        withMigratedDb { ds ->
            val repo = PostgresOppgaveRepository(ds)

            shouldNotThrowAny {
                repo.lagre(testOppgave)
                repo.lagre(testOppgave)
            }
        }
    }

    @Test
    fun `Skal kunne lagre og hente en oppgave`() {
        val testOppgave = lagOppgave()
        withMigratedDb { ds ->
            val repo = PostgresOppgaveRepository(ds)
            repo.lagre(testOppgave)
            val oppgaveFraDatabase = repo.hentOppgave(testOppgave.oppgaveId)
            oppgaveFraDatabase shouldBe testOppgave
        }
    }

    @Test
    fun `Skal kunne lagre og hente tilstandslogg for en spesifikk oppgave`() {
        val nå = LocalDateTime.now()

        val tilstandslogg =
            Tilstandslogg(
                mutableListOf(
                    Tilstandsendring(
                        tilstand = KLAR_TIL_KONTROLL,
                        hendelse = KlarTilKontrollHendelse(oppgaveId = oppgaveIdTest, saksbehandler),
                        tidspunkt = nå.minusDays(2).truncatedTo(ChronoUnit.SECONDS),
                    ),
                    Tilstandsendring(
                        tilstand = UNDER_KONTROLL,
                        hendelse = ToTrinnskontrollHendelse(oppgaveId = oppgaveIdTest, beslutter.navIdent, beslutter),
                        tidspunkt = nå.minusDays(1).truncatedTo(ChronoUnit.SECONDS),
                    ),
                    Tilstandsendring(
                        tilstand = FERDIG_BEHANDLET,
                        hendelse =
                            GodkjentBehandlingHendelse(
                                oppgaveId = oppgaveIdTest,
                                meldingOmVedtak = "<h1>This is HTML</h1>",
                                utførtAv = beslutter,
                            ),
                        tidspunkt = nå.truncatedTo(ChronoUnit.SECONDS),
                    ),
                ),
            )
        val testOppgave = lagOppgave(tilstandslogg = tilstandslogg, oppgaveId = oppgaveIdTest)
        withMigratedDb { ds ->
            val repo = PostgresOppgaveRepository(ds)
            repo.lagre(testOppgave)
            val oppgaveFraDatabase = repo.hentOppgave(testOppgave.oppgaveId)
            oppgaveFraDatabase.tilstandslogg.size shouldBe testOppgave.tilstandslogg.size
            oppgaveFraDatabase.tilstandslogg.forEachIndexed { index, tilstandsendring ->
                val testOppgaveTilstandsendring = testOppgave.tilstandslogg[index]
                tilstandsendring.tilstand shouldBe testOppgaveTilstandsendring.tilstand
                tilstandsendring.id shouldBe testOppgaveTilstandsendring.id
                tilstandsendring.hendelse shouldBe testOppgaveTilstandsendring.hendelse
                tilstandsendring.tidspunkt.truncatedTo(ChronoUnit.MILLIS) shouldBe
                    testOppgaveTilstandsendring.tidspunkt.truncatedTo(ChronoUnit.MILLIS)
            }
        }
    }

    @Test
    fun `Skal kunne endre tilstand på en oppgave`() {
        val testOppgave = lagOppgave(tilstand = KlarTilBehandling)
        withMigratedDb { ds ->
            val repo = PostgresOppgaveRepository(ds)

            repo.lagre(testOppgave)
            repo.hentOppgave(testOppgave.oppgaveId).tilstand().type shouldBe KLAR_TIL_BEHANDLING

            repo.lagre(testOppgave.copy(tilstand = FerdigBehandlet))
            repo.hentOppgave(testOppgave.oppgaveId).tilstand().type shouldBe FERDIG_BEHANDLET
        }
    }

    @Test
    fun `CRUD på oppgave med PAAVENT tilstand`() {
        val testOppgave = lagOppgave(tilstand = UnderBehandling)
        val utsattTil = LocalDate.now().plusDays(1)
        withMigratedDb { ds ->
            val repo = PostgresOppgaveRepository(ds)

            repo.lagre(
                testOppgave.copy(
                    tilstand = Oppgave.PaaVent,
                    utsattTil = utsattTil,
                ),
            )
            repo.hentOppgave(testOppgave.oppgaveId).let { oppgave: Oppgave ->
                oppgave.tilstand() shouldBe Oppgave.PaaVent
                oppgave.utsattTil() shouldBe utsattTil
            }
        }
    }

    @Test
    fun `Skal kunne søke etter oppgaver filtrert på tilstand`() {
        val oppgaveKlarTilBehandling = lagOppgave(tilstand = KlarTilBehandling)
        val oppgaveFerdigBehandlet = lagOppgave(tilstand = FerdigBehandlet)

        withMigratedDb { ds ->
            val repo = PostgresOppgaveRepository(ds)
            repo.lagre(oppgaveKlarTilBehandling)
            repo.lagre(oppgaveFerdigBehandlet)

            repo.hentAlleOppgaverMedTilstand(FERDIG_BEHANDLET).let { oppgaver ->
                oppgaver.size shouldBe 1
                oppgaver.single().oppgaveId shouldBe oppgaveFerdigBehandlet.oppgaveId
            }

            repo.hentAlleOppgaverMedTilstand(KLAR_TIL_BEHANDLING).let { oppgaver ->
                oppgaver.size shouldBe 1
                oppgaver.single().oppgaveId shouldBe oppgaveKlarTilBehandling.oppgaveId
            }
        }
    }

    @Test
    fun `Skal kunne hente alle oppgaver for en gitt person`() {
        val ola =
            Person(
                ident = "12345678910",
                skjermesSomEgneAnsatte = false,
                adressebeskyttelseGradering = UGRADERT,
            )
        val kari =
            Person(
                ident = "10987654321",
                skjermesSomEgneAnsatte = false,
                adressebeskyttelseGradering = UGRADERT,
            )

        val oppgave1TilOla = lagOppgave(person = ola, tilstand = KlarTilBehandling)
        val oppgave2TilOla = lagOppgave(person = ola, tilstand = FerdigBehandlet)
        val oppgave1TilKari = lagOppgave(person = kari, tilstand = FerdigBehandlet)

        withMigratedDb { ds ->
            val repo = PostgresOppgaveRepository(ds)
            repo.lagre(oppgave1TilOla)
            repo.lagre(oppgave2TilOla)
            repo.lagre(oppgave1TilKari)

            repo.finnOppgaverFor(ola.ident) shouldBe listOf(oppgave1TilOla, oppgave2TilOla)
            repo.finnOppgaverFor(kari.ident) shouldBe listOf(oppgave1TilKari)
        }
    }

    @Test
    fun `Skal hente oppgaveId fra behandlingId`() {
        val behandling = lagBehandling()
        val oppgave = lagOppgave(behandling = behandling)

        withMigratedDb { ds ->
            val repo = PostgresOppgaveRepository(ds)
            repo.lagre(oppgave)
            repo.lagre(behandling)

            repo.hentOppgaveIdFor(behandlingId = behandling.behandlingId) shouldBe oppgave.oppgaveId
            repo.hentOppgaveIdFor(behandlingId = UUIDv7.ny()) shouldBe null
        }
    }

    @Test
    fun `Skal kunne søke etter oppgaver filtrert på emneknagger`() {
        withMigratedDb { ds ->
            val repo = PostgresOppgaveRepository(ds)
            val oppgave1 = lagOppgave(emneknagger = setOf("hubba", "bubba"))
            val oppgave2 = lagOppgave(emneknagger = setOf("hubba"))
            val oppgave3 = lagOppgave(emneknagger = emptySet())

            repo.lagre(oppgave1)
            repo.lagre(oppgave2)
            repo.lagre(oppgave3)

            repo.søk(
                Søkefilter(
                    tilstand = Oppgave.Tilstand.Type.entries.toSet(),
                    periode = UBEGRENSET_PERIODE,
                    emneknagg = emptySet(),
                ),
            ) shouldBe listOf(oppgave1, oppgave2, oppgave3)

            repo.søk(
                Søkefilter(
                    tilstand = Oppgave.Tilstand.Type.entries.toSet(),
                    periode = UBEGRENSET_PERIODE,
                    emneknagg = setOf("hubba"),
                ),
            ) shouldBe listOf(oppgave1, oppgave2)

            repo.søk(
                Søkefilter(
                    tilstand = Oppgave.Tilstand.Type.entries.toSet(),
                    periode = UBEGRENSET_PERIODE,
                    emneknagg = setOf("bubba"),
                ),
            ) shouldBe listOf(oppgave1)

            repo.søk(
                Søkefilter(
                    tilstand = Oppgave.Tilstand.Type.entries.toSet(),
                    periode = UBEGRENSET_PERIODE,
                    emneknagg = setOf("bubba", "hubba"),
                ),
            ) shouldBe listOf(oppgave1, oppgave2)
        }
    }

    @Test
    fun `Skal kunne søke etter oppgaver tildel en gitt saksbehandler`() {
        val enUkeSiden = opprettetNå.minusDays(7)
        val saksbehandler1 = "saksbehandler1"
        val saksbehandler2 = "saksbehandler2"

        withMigratedDb { ds ->
            val repo = PostgresOppgaveRepository(ds)
            val oppgave1 = lagOppgave(UnderBehandling, enUkeSiden, saksbehandler1)
            val oppgave2 = lagOppgave(UnderBehandling, saksbehandlerIdent = saksbehandler2)
            val oppgave3 = lagOppgave(FerdigBehandlet, saksbehandlerIdent = saksbehandler2)
            val oppgave4 = lagOppgave(UnderBehandling, saksbehandlerIdent = null)

            repo.lagre(oppgave1)
            repo.lagre(oppgave2)
            repo.lagre(oppgave3)
            repo.lagre(oppgave4)

            repo.søk(
                Søkefilter(
                    tilstand = Oppgave.Tilstand.Type.entries.toSet(),
                    periode = UBEGRENSET_PERIODE,
                    saksbehandlerIdent = saksbehandler1,
                ),
            ).size shouldBe 1

            repo.søk(
                Søkefilter(
                    tilstand = Oppgave.Tilstand.Type.entries.toSet(),
                    periode = UBEGRENSET_PERIODE,
                    saksbehandlerIdent = saksbehandler2,
                ),
            ).size shouldBe 2

            repo.søk(
                Søkefilter(
                    tilstand = Oppgave.Tilstand.Type.entries.toSet(),
                    periode = UBEGRENSET_PERIODE,
                    saksbehandlerIdent = null,
                ),
            ).size shouldBe 4
        }
    }

    @Test
    fun `Skal kunne søke etter oppgaver filtrert på tilstand og opprettet`() {
        val enUkeSiden = opprettetNå.minusDays(7)

        withMigratedDb { ds ->
            val repo = PostgresOppgaveRepository(ds)
            val oppgaveUnderBehandlingEnUkeGammel =
                lagOppgave(UnderBehandling, opprettet = enUkeSiden, saksbehandlerIdent = saksbehandler.navIdent)
            val oppgaveKlarTilBehandlingIDag = lagOppgave(KlarTilBehandling)
            val oppgaveKlarTilBehandlingIGår = lagOppgave(KlarTilBehandling, opprettet = opprettetNå.minusDays(1))
            val oppgaveOpprettetIDag = lagOppgave(Opprettet)
            repo.lagre(oppgaveUnderBehandlingEnUkeGammel)
            repo.lagre(oppgaveKlarTilBehandlingIDag)
            repo.lagre(oppgaveKlarTilBehandlingIGår)
            repo.lagre(oppgaveOpprettetIDag)

            repo.søk(
                Søkefilter(
                    tilstand = setOf(UNDER_BEHANDLING),
                    periode = UBEGRENSET_PERIODE,
                ),
            ).single() shouldBe oppgaveUnderBehandlingEnUkeGammel

            repo.søk(
                Søkefilter(
                    tilstand = setOf(KLAR_TIL_BEHANDLING, UNDER_BEHANDLING),
                    periode = UBEGRENSET_PERIODE,
                ),
            ).size shouldBe 3

            repo.søk(
                Søkefilter(
                    periode = UBEGRENSET_PERIODE,
                    tilstand = Oppgave.Tilstand.Type.søkbareTyper,
                    saksbehandlerIdent = null,
                    personIdent = null,
                    oppgaveId = null,
                    behandlingId = null,
                ),
            ).let {
                it.size shouldBe 3
                it.map { oppgave -> oppgave.tilstand().type }.toSet() shouldBe
                    setOf(
                        UNDER_BEHANDLING,
                        KLAR_TIL_BEHANDLING,
                    )
            }

            repo.søk(
                Søkefilter(
                    tilstand = setOf(KLAR_TIL_BEHANDLING),
                    periode =
                        Periode(
                            fom = enUkeSiden.plusDays(1).toLocalDate(),
                            tom = enUkeSiden.plusDays(2).toLocalDate(),
                        ),
                ),
            ).size shouldBe 0

            repo.søk(
                Søkefilter(
                    tilstand = setOf(UNDER_BEHANDLING),
                    periode =
                        Periode(
                            fom = enUkeSiden.minusDays(1).toLocalDate(),
                            tom = enUkeSiden.plusDays(2).toLocalDate(),
                        ),
                ),
            ).size shouldBe 1

            repo.søk(
                Søkefilter(
                    tilstand = setOf(KLAR_TIL_BEHANDLING),
                    periode =
                        Periode(
                            fom = opprettetNå.toLocalDate(),
                            tom = opprettetNå.toLocalDate(),
                        ),
                ),
            ).size shouldBe 1
        }
    }

    @Test
    fun `Skal kunne søke etter oppgaver opprettet en bestemt dato, uavhengig av tid på døgnet`() {
        withMigratedDb { ds ->
            val iDag = LocalDate.now()
            val iGår: LocalDate = iDag.minusDays(1)
            val iForgårs = iDag.minusDays(2)
            val iForgårsSåSeintPåDagenSomMulig = LocalDateTime.of(iForgårs, LocalTime.MAX)
            val iGårSåTidligPåDagenSomMulig = LocalDateTime.of(iGår, LocalTime.MIN)
            val iGårSåSeintPåDagenSomMulig = LocalDateTime.of(iGår, LocalTime.MAX)
            val iDagSåTidligPåDagenSomMulig = LocalDateTime.of(iDag, LocalTime.MIN)
            val repo = PostgresOppgaveRepository(ds)
            val oppgaveOpprettetSeintForgårs =
                lagOppgave(KlarTilBehandling, opprettet = iForgårsSåSeintPåDagenSomMulig)
            val oppgaveOpprettetTidligIGår = lagOppgave(KlarTilBehandling, opprettet = iGårSåTidligPåDagenSomMulig)
            val oppgaveOpprettetSeintIGår = lagOppgave(KlarTilBehandling, opprettet = iGårSåSeintPåDagenSomMulig)
            val oppgaveOpprettetTidligIDag = lagOppgave(KlarTilBehandling, opprettet = iDagSåTidligPåDagenSomMulig)

            repo.lagre(oppgaveOpprettetSeintForgårs)
            repo.lagre(oppgaveOpprettetTidligIGår)
            repo.lagre(oppgaveOpprettetSeintIGår)
            repo.lagre(oppgaveOpprettetTidligIDag)

            val oppgaver =
                repo.søk(
                    Søkefilter(
                        tilstand = setOf(KLAR_TIL_BEHANDLING),
                        periode = Periode(fom = iGår, tom = iGår),
                    ),
                )
            oppgaver.size shouldBe 2
            oppgaver.contains(oppgaveOpprettetTidligIGår)
            oppgaver.contains(oppgaveOpprettetSeintIGår)
        }
    }

    @Test
    fun `Skal hente en oppgave basert på behandlingId`() {
        withMigratedDb { ds ->
            val repo = PostgresOppgaveRepository(ds)
            val oppgave = lagOppgave()
            repo.lagre(oppgave)
            repo.hentOppgaveFor(oppgave.behandlingId) shouldBe oppgave

            assertThrows<DataNotFoundException> {
                repo.hentOppgaveFor(behandlingId = UUIDv7.ny())
            }
        }
    }

    @Test
    fun `Skal finne en oppgave basert på behandlingId hvis den finnes`() {
        withMigratedDb { ds ->
            val repo = PostgresOppgaveRepository(ds)
            val oppgave = lagOppgave()
            repo.lagre(oppgave)
            repo.finnOppgaveFor(oppgave.behandlingId) shouldBe oppgave
            repo.finnOppgaveFor(behandlingId = UUIDv7.ny()) shouldBe null
        }
    }

    @Test
    fun `Sjekk om fnre eksisterer i vårt system`() {
        withMigratedDb { ds ->
            val repo = PostgresOppgaveRepository(ds)
            val (fnr1, fnr2, fnr3) = Triple("12345678910", "10987654321", "12345678931")

            repo.lagre(lagPerson(fnr1))
            repo.lagre(lagPerson(fnr2))
            repo.lagre(lagPerson("12345678941"))
            repo.eksistererIDPsystem(setOf(fnr1, fnr2, fnr3)) shouldBe setOf(fnr1, fnr2)
        }
    }

    @Test
    fun `Hent adressegraderingsbeskyttelse for person gitt oppgave`() {
        withMigratedDb { ds ->
            val repo = PostgresOppgaveRepository(ds)
            val oppgave =
                lagOppgave(
                    person =
                        lagPerson(
                            ident = testPerson.ident,
                            addresseBeskyttelseGradering = STRENGT_FORTROLIG,
                        ),
                )
            repo.lagre(oppgave)
            repo.adresseGraderingForPerson(oppgave.oppgaveId) shouldBe STRENGT_FORTROLIG
        }
    }
}
