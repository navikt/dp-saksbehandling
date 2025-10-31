package no.nav.dagpenger.saksbehandling.db.oppgave

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.FORTROLIG
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.STRENGT_FORTROLIG
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.Applikasjon
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.Emneknagg
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.OppgaveTilstandslogg
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.Sak
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.TilgangType
import no.nav.dagpenger.saksbehandling.Tilstandsendring
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.UtløstAvType
import no.nav.dagpenger.saksbehandling.adressebeskyttelseTilganger
import no.nav.dagpenger.saksbehandling.db.DBTestHelper
import no.nav.dagpenger.saksbehandling.hendelser.GodkjentBehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.NesteOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.NotatHendelse
import no.nav.dagpenger.saksbehandling.hendelser.PåVentFristUtgåttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SendTilKontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SkriptHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SøknadsbehandlingOpprettetHendelse
import no.nav.dagpenger.saksbehandling.hendelser.TomHendelse
import no.nav.dagpenger.saksbehandling.hendelser.UtsettOppgaveHendelse
import no.nav.dagpenger.saksbehandling.lagBehandling
import no.nav.dagpenger.saksbehandling.lagOppgave
import no.nav.dagpenger.saksbehandling.lagPerson
import no.nav.dagpenger.saksbehandling.opprettetNå
import no.nav.dagpenger.saksbehandling.testPerson
import org.junit.jupiter.api.Test
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
    val person =
        Person(
            id = testPerson.id,
            ident = testPerson.ident,
            skjermesSomEgneAnsatte = false,
            adressebeskyttelseGradering = UGRADERT,
        )

    @Test
    fun `Det finnes ikke flere ledige oppgaver`() {
        val behandling =
            Behandling(
                behandlingId = UUIDv7.ny(),
                utløstAv = UtløstAvType.SØKNAD,
                opprettet = LocalDateTime.now(),
                hendelse = TomHendelse,
            )

        DBTestHelper.Companion.withBehandling(behandling = behandling) { ds ->
            val repo = PostgresOppgaveRepository(ds)
            val saksbehandler =
                Saksbehandler(
                    navIdent = "NAVIdent2",
                    grupper = emptySet(),
                )
            val oppgave = lagOppgave(tilstand = Oppgave.FerdigBehandlet, behandlingId = behandling.behandlingId)
            repo.lagre(oppgave)
            repo.tildelOgHentNesteOppgave(
                nesteOppgaveHendelse =
                    NesteOppgaveHendelse(
                        ansvarligIdent = saksbehandler.navIdent,
                        utførtAv = saksbehandler,
                    ),
                filter =
                    TildelNesteOppgaveFilter(
                        periode = Periode.Companion.UBEGRENSET_PERIODE,
                        emneknagger = emptySet(),
                        adressebeskyttelseTilganger = setOf(FORTROLIG),
                        navIdent = saksbehandler.navIdent,
                    ),
            ) shouldBe null
        }
    }

    @Test
    fun `Tildel neste ledige kontroll-oppgave ved søk på tilstand KLAR_TIL_KONTROLL`() {
        val oppgaveIdKlarTilKontroll = UUIDv7.ny()
        val søknadBehandlingKlarTilBehandling =
            lagBehandling(type = UtløstAvType.SØKNAD, opprettet = opprettetNå.minusDays(2))
        val søknadBehandlingKlarTilKontroll =
            lagBehandling(type = UtløstAvType.SØKNAD, opprettet = opprettetNå.minusDays(1))
        DBTestHelper.Companion.withBehandlinger(
            person = testPerson,
            behandlinger = listOf(søknadBehandlingKlarTilBehandling, søknadBehandlingKlarTilKontroll),
        ) { ds ->
            val repo = PostgresOppgaveRepository(ds)

            lagOppgave(
                tilstand = Oppgave.KlarTilBehandling,
                opprettet = søknadBehandlingKlarTilBehandling.opprettet,
                behandlingId = søknadBehandlingKlarTilBehandling.behandlingId,
                person = testPerson,
            ).also { repo.lagre(it) }

            lagOppgave(
                oppgaveId = oppgaveIdKlarTilKontroll,
                tilstand = Oppgave.KlarTilKontroll,
                opprettet = søknadBehandlingKlarTilKontroll.opprettet,
                behandlingId = søknadBehandlingKlarTilKontroll.behandlingId,
                person = testPerson,
                tilstandslogg =
                    OppgaveTilstandslogg(
                        Tilstandsendring(
                            tilstand = Oppgave.Tilstand.Type.UNDER_BEHANDLING,
                            hendelse =
                                SettOppgaveAnsvarHendelse(
                                    oppgaveId = oppgaveIdKlarTilKontroll,
                                    ansvarligIdent = saksbehandler.navIdent,
                                    utførtAv = saksbehandler,
                                ),
                            tidspunkt = opprettetNå,
                        ),
                        Tilstandsendring(
                            tilstand = Oppgave.Tilstand.Type.KLAR_TIL_KONTROLL,
                            hendelse =
                                SendTilKontrollHendelse(
                                    oppgaveId = oppgaveIdKlarTilKontroll,
                                    utførtAv = saksbehandler,
                                ),
                            tidspunkt = opprettetNå,
                        ),
                    ),
            ).also { repo.lagre(it) }

            val nesteOppgave =
                repo.tildelOgHentNesteOppgave(
                    nesteOppgaveHendelse =
                        NesteOppgaveHendelse(
                            ansvarligIdent = beslutter.navIdent,
                            utførtAv = beslutter,
                        ),
                    filter =
                        TildelNesteOppgaveFilter(
                            periode = Periode.Companion.UBEGRENSET_PERIODE,
                            emneknagger = emptySet(),
                            tilstander = setOf(Oppgave.Tilstand.Type.KLAR_TIL_KONTROLL),
                            egneAnsatteTilgang = false,
                            adressebeskyttelseTilganger = setOf(UGRADERT),
                            navIdent = beslutter.navIdent,
                            harBeslutterRolle = beslutter.tilganger.contains(TilgangType.BESLUTTER),
                        ),
                )!!

            nesteOppgave.behandlingId shouldBe søknadBehandlingKlarTilKontroll.behandlingId
            nesteOppgave.behandlerIdent shouldBe beslutter.navIdent
            nesteOppgave.tilstand().type shouldBe Oppgave.Tilstand.Type.UNDER_KONTROLL
        }
    }

    @Test
    fun `Tildel neste ledige klage-oppgave`() {
        val klageBehandling = lagBehandling(type = UtløstAvType.KLAGE)
        val søknadBehandling = lagBehandling(type = UtløstAvType.SØKNAD)
        DBTestHelper.Companion.withBehandlinger(
            person = testPerson,
            behandlinger = listOf(klageBehandling, søknadBehandling),
        ) { ds ->
            val repo = PostgresOppgaveRepository(ds)

            lagOppgave(
                tilstand = Oppgave.KlarTilBehandling,
                opprettet = opprettetNå.minusDays(1),
                behandlingId = søknadBehandling.behandlingId,
                person = testPerson,
            ).also { repo.lagre(it) }

            val klageOppgave =
                lagOppgave(
                    tilstand = Oppgave.KlarTilBehandling,
                    opprettet = opprettetNå,
                    behandlingId = klageBehandling.behandlingId,
                    person = testPerson,
                    utløstAvType = UtløstAvType.KLAGE,
                ).also { repo.lagre(it) }

            val nesteOppgave =
                repo.tildelOgHentNesteOppgave(
                    nesteOppgaveHendelse =
                        NesteOppgaveHendelse(
                            ansvarligIdent = saksbehandler.navIdent,
                            utførtAv = saksbehandler,
                        ),
                    filter =
                        TildelNesteOppgaveFilter(
                            periode = Periode.Companion.UBEGRENSET_PERIODE,
                            emneknagger = emptySet(),
                            utløstAvTyper = setOf(UtløstAvType.KLAGE),
                            egneAnsatteTilgang = false,
                            adressebeskyttelseTilganger = setOf(UGRADERT),
                            navIdent = saksbehandler.navIdent,
                        ),
                )!!

            nesteOppgave.oppgaveId shouldBe klageOppgave.oppgaveId
            nesteOppgave.behandlerIdent shouldBe saksbehandler.navIdent
            nesteOppgave.tilstand().type shouldBe Oppgave.Tilstand.Type.UNDER_BEHANDLING
        }
    }

    @Test
    fun `Tildel neste ledige oppgave som ikke gjelder egne ansatte`() {
        DBTestHelper.Companion.withMigratedDb { ds ->
            val eldsteOppgaveMedSkjermingSomEgneAnsatte =
                this.leggTilOppgave(
                    tilstand = Oppgave.KlarTilBehandling,
                    opprettet = opprettetNå.minusDays(5),
                    person =
                        Person(
                            ident = "12345123451",
                            skjermesSomEgneAnsatte = true,
                            adressebeskyttelseGradering = UGRADERT,
                        ),
                )

            val eldsteOppgaveUtenSkjermingAvEgenAnsatt =
                this.leggTilOppgave(
                    tilstand = Oppgave.KlarTilBehandling,
                    opprettet = opprettetNå.minusDays(1),
                    person =
                        Person(
                            ident = "11111222222",
                            skjermesSomEgneAnsatte = false,
                            adressebeskyttelseGradering = UGRADERT,
                        ),
                )

            this.leggTilOppgave(
                tilstand = Oppgave.KlarTilBehandling,
                opprettet = opprettetNå,
                person =
                    Person(
                        ident = "11111333333",
                        skjermesSomEgneAnsatte = false,
                        adressebeskyttelseGradering = UGRADERT,
                    ),
            )

            val repo = PostgresOppgaveRepository(ds)

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
                            periode = Periode.Companion.UBEGRENSET_PERIODE,
                            emneknagger = emptySet(),
                            egneAnsatteTilgang = false,
                            adressebeskyttelseTilganger = setOf(UGRADERT),
                            navIdent = saksbehandlerUtenTilgangTilEgneAnsatte.navIdent,
                        ),
                )!!
            nesteOppgave.oppgaveId shouldBe eldsteOppgaveUtenSkjermingAvEgenAnsatt.oppgaveId
            nesteOppgave.behandlerIdent shouldBe saksbehandlerUtenTilgangTilEgneAnsatte.navIdent
            nesteOppgave.tilstand().type shouldBe Oppgave.Tilstand.Type.UNDER_BEHANDLING

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
                            periode = Periode.Companion.UBEGRENSET_PERIODE,
                            emneknagger = emptySet(),
                            egneAnsatteTilgang = true,
                            adressebeskyttelseTilganger = setOf(UGRADERT),
                            navIdent = saksbehandlerMedTilgangTilEgneAnsatte.navIdent,
                        ),
                )!!

            nesteOppgaveMedTilgang.oppgaveId shouldBe eldsteOppgaveMedSkjermingSomEgneAnsatte.oppgaveId
            nesteOppgaveMedTilgang.behandlerIdent shouldBe saksbehandlerMedTilgangTilEgneAnsatte.navIdent
            nesteOppgaveMedTilgang.tilstand().type shouldBe Oppgave.Tilstand.Type.UNDER_BEHANDLING
        }
    }

    @Test
    fun `Tildel neste ledige oppgave som ikke gjelder adressebeskyttede personer`() {
        DBTestHelper.Companion.withMigratedDb { ds ->

            val eldsteOppgaveMedAdressebeskyttelse =
                this.leggTilOppgave(
                    tilstand = Oppgave.KlarTilBehandling,
                    opprettet = opprettetNå.minusDays(5),
                    person =
                        Person(
                            ident = "12345123451",
                            skjermesSomEgneAnsatte = false,
                            adressebeskyttelseGradering = FORTROLIG,
                        ),
                )

            val eldsteOppgaveUtenAdressebeskyttelse =
                this.leggTilOppgave(
                    tilstand = Oppgave.KlarTilBehandling,
                    opprettet = opprettetNå.minusDays(1),
                    person =
                        Person(
                            ident = "11111222222",
                            skjermesSomEgneAnsatte = false,
                            adressebeskyttelseGradering = UGRADERT,
                        ),
                )

            val saksbehandlernUtenTilgangTilAdressebeskyttede =
                Saksbehandler(
                    navIdent = "NAVIdent2",
                    grupper = emptySet(),
                    tilganger = emptySet(),
                )

            val repo = PostgresOppgaveRepository(ds)
            val nesteOppgave =
                repo.tildelOgHentNesteOppgave(
                    nesteOppgaveHendelse =
                        NesteOppgaveHendelse(
                            ansvarligIdent = saksbehandlernUtenTilgangTilAdressebeskyttede.navIdent,
                            utførtAv = saksbehandlernUtenTilgangTilAdressebeskyttede,
                        ),
                    filter =
                        TildelNesteOppgaveFilter(
                            periode = Periode.Companion.UBEGRENSET_PERIODE,
                            emneknagger = emptySet(),
                            egneAnsatteTilgang = false,
                            adressebeskyttelseTilganger = setOf(UGRADERT),
                            harBeslutterRolle = false,
                            navIdent = saksbehandlernUtenTilgangTilAdressebeskyttede.navIdent,
                        ),
                )
            requireNotNull(nesteOppgave)
            nesteOppgave.oppgaveId shouldBe eldsteOppgaveUtenAdressebeskyttelse.oppgaveId
            nesteOppgave.behandlerIdent shouldBe saksbehandlernUtenTilgangTilAdressebeskyttede.navIdent
            nesteOppgave.tilstand().type shouldBe Oppgave.Tilstand.Type.UNDER_BEHANDLING

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
                            periode = Periode.Companion.UBEGRENSET_PERIODE,
                            emneknagger = emptySet(),
                            egneAnsatteTilgang = true,
                            adressebeskyttelseTilganger =
                                setOf(
                                    UGRADERT,
                                    FORTROLIG,
                                ),
                            navIdent = saksbehandlerMedTilgangTilEgneAnsatte.navIdent,
                        ),
                )!!

            nesteOppgaveMedTilgang.oppgaveId shouldBe eldsteOppgaveMedAdressebeskyttelse.oppgaveId
            nesteOppgaveMedTilgang.behandlerIdent shouldBe saksbehandlerMedTilgangTilEgneAnsatte.navIdent
            nesteOppgaveMedTilgang.tilstand().type shouldBe Oppgave.Tilstand.Type.UNDER_BEHANDLING
        }
    }

    @Test
    fun `Ved tildeling av neste oppgave, skal det lagres en tilstandsendring i tilstandsendringsloggen`() {
        DBTestHelper.Companion.withMigratedDb { ds ->
            val oppgave =
                this.leggTilOppgave(
                    tilstand = Oppgave.KlarTilBehandling,
                    opprettet = opprettetNå,
                    emneknagger = setOf("Testknagg"),
                )

            val repo = PostgresOppgaveRepository(ds)
            val saksbehandler =
                Saksbehandler(
                    navIdent = "NAVIdent",
                    grupper = emptySet(),
                )

            val nesteOppgaveHendelse =
                NesteOppgaveHendelse(
                    ansvarligIdent = saksbehandler.navIdent,
                    utførtAv = saksbehandler,
                )
            val rehydrertOppgave = repo.hentOppgave(oppgave.oppgaveId)
            val antallTilstandsendringer = rehydrertOppgave.tilstandslogg.size

            val filter =
                TildelNesteOppgaveFilter(
                    periode = Periode.Companion.UBEGRENSET_PERIODE,
                    emneknagger = setOf("Testknagg"),
                    adressebeskyttelseTilganger = setOf(UGRADERT),
                    navIdent = saksbehandler.navIdent,
                )
            val nesteOppgave = repo.tildelOgHentNesteOppgave(nesteOppgaveHendelse, filter)
            nesteOppgave!!.tilstandslogg.size shouldBe antallTilstandsendringer + 1
        }
    }

    @Test
    fun `Skal ikke få tildelt kontrolloppgave som man selv har saksbehandlet`() {
        DBTestHelper.Companion.withMigratedDb { ds ->
            val saksbehandlerUtført =
                Saksbehandler(
                    navIdent = "saksbehandlerUtført",
                    grupper = emptySet(),
                    tilganger = setOf(TilgangType.SAKSBEHANDLER, TilgangType.BESLUTTER),
                )

            val annenBeslutter =
                Saksbehandler(
                    navIdent = "annenBeslutter",
                    grupper = emptySet(),
                    tilganger = setOf(TilgangType.SAKSBEHANDLER, TilgangType.BESLUTTER),
                )
            val oppgaveId = UUIDv7.ny()

            val tilstandsloggUnderBehandling =
                OppgaveTilstandslogg(
                    tilstandsendringer =
                        mutableListOf(
                            Tilstandsendring(
                                tilstand = Oppgave.Tilstand.Type.UNDER_BEHANDLING,
                                hendelse =
                                    NesteOppgaveHendelse(
                                        ansvarligIdent = annenBeslutter.navIdent,
                                        utførtAv = annenBeslutter,
                                    ),
                                tidspunkt = LocalDateTime.now().minusDays(3),
                            ),
                            Tilstandsendring(
                                tilstand = Oppgave.Tilstand.Type.UNDER_BEHANDLING,
                                hendelse =
                                    NesteOppgaveHendelse(
                                        ansvarligIdent = saksbehandlerUtført.navIdent,
                                        utførtAv = saksbehandlerUtført,
                                    ),
                                tidspunkt = LocalDateTime.now().minusDays(2),
                            ),
                            Tilstandsendring(
                                tilstand = Oppgave.Tilstand.Type.PAA_VENT,
                                hendelse =
                                    UtsettOppgaveHendelse(
                                        oppgaveId = oppgaveId,
                                        navIdent = saksbehandlerUtført.navIdent,
                                        utsattTil = LocalDate.now(),
                                        beholdOppgave = true,
                                        utførtAv = saksbehandlerUtført,
                                    ),
                                tidspunkt = LocalDateTime.now().minusDays(1),
                            ),
                            Tilstandsendring(
                                tilstand = Oppgave.Tilstand.Type.UNDER_BEHANDLING,
                                hendelse =
                                    PåVentFristUtgåttHendelse(
                                        oppgaveId = oppgaveId,
                                        utførtAv = Applikasjon("dp-saksbehandling"),
                                    ),
                                tidspunkt = LocalDateTime.now().minusHours(1),
                            ),
                            Tilstandsendring(
                                tilstand = Oppgave.Tilstand.Type.KLAR_TIL_KONTROLL,
                                hendelse =
                                    SendTilKontrollHendelse(
                                        oppgaveId = oppgaveId,
                                        utførtAv = saksbehandlerUtført,
                                    ),
                                tidspunkt = LocalDateTime.now(),
                            ),
                        ),
                )

            val oppgave =
                this.leggTilOppgave(
                    id = oppgaveId,
                    tilstand = Oppgave.KlarTilKontroll,
                    tilstandslogg = tilstandsloggUnderBehandling,
                )

            val repo = PostgresOppgaveRepository(ds)

            repo.tildelOgHentNesteOppgave(
                nesteOppgaveHendelse =
                    NesteOppgaveHendelse(
                        ansvarligIdent = saksbehandlerUtført.navIdent,
                        utførtAv = saksbehandlerUtført,
                    ),
                filter =
                    TildelNesteOppgaveFilter(
                        periode = Periode.Companion.UBEGRENSET_PERIODE,
                        emneknagger = emptySet(),
                        egneAnsatteTilgang = saksbehandlerUtført.tilganger.contains(TilgangType.EGNE_ANSATTE),
                        adressebeskyttelseTilganger = saksbehandlerUtført.adressebeskyttelseTilganger(),
                        harBeslutterRolle = saksbehandlerUtført.tilganger.contains(TilgangType.BESLUTTER),
                        navIdent = saksbehandlerUtført.navIdent,
                    ),
            ) shouldBe null

            repo.tildelOgHentNesteOppgave(
                nesteOppgaveHendelse =
                    NesteOppgaveHendelse(
                        ansvarligIdent = annenBeslutter.navIdent,
                        utførtAv = annenBeslutter,
                    ),
                filter =
                    TildelNesteOppgaveFilter(
                        periode = Periode.Companion.UBEGRENSET_PERIODE,
                        emneknagger = emptySet(),
                        egneAnsatteTilgang = annenBeslutter.tilganger.contains(TilgangType.EGNE_ANSATTE),
                        adressebeskyttelseTilganger = annenBeslutter.adressebeskyttelseTilganger(),
                        harBeslutterRolle = annenBeslutter.tilganger.contains(TilgangType.BESLUTTER),
                        navIdent = annenBeslutter.navIdent,
                    ),
            ).let {
                assertSoftly {
                    require(it != null) { "Skal finne en oppgave" }
                    it.oppgaveId shouldBe oppgave.oppgaveId
                    it.behandlerIdent shouldBe annenBeslutter.navIdent
                    it.tilstand() shouldBe Oppgave.UnderKontroll()
                }
            }
        }
    }

    @Test
    fun `Tildeling av neste oppgave ut fra søkefilter og tilganger`() {
        DBTestHelper.Companion.withMigratedDb { ds ->
            val saksbehandlerUtført =
                Saksbehandler(
                    navIdent = "saksbehandlerUtført",
                    grupper = emptySet(),
                    tilganger = setOf(TilgangType.SAKSBEHANDLER),
                )

            fun tilstandsloggUnderBehandling() =
                OppgaveTilstandslogg(
                    Tilstandsendring(
                        tilstand = Oppgave.Tilstand.Type.UNDER_BEHANDLING,
                        hendelse =
                            NesteOppgaveHendelse(
                                ansvarligIdent = saksbehandlerUtført.navIdent,
                                utførtAv = saksbehandlerUtført,
                            ),
                    ),
                )

            val testSaksbehandler =
                Saksbehandler(
                    navIdent = "saksbehandler",
                    grupper = emptySet(),
                    tilganger = setOf(TilgangType.SAKSBEHANDLER),
                )
            val vanligBeslutter =
                Saksbehandler(
                    navIdent = "vanligBeslutter",
                    grupper = emptySet(),
                    tilganger = setOf(TilgangType.SAKSBEHANDLER, TilgangType.BESLUTTER),
                )
            val beslutterEgneAnsatte =
                Saksbehandler(
                    navIdent = "beslutterEgneAnsatte",
                    grupper = emptySet(),
                    tilganger = setOf(TilgangType.SAKSBEHANDLER, TilgangType.BESLUTTER, TilgangType.EGNE_ANSATTE),
                )
            val beslutterFortroligAdresse =
                Saksbehandler(
                    navIdent = "beslutterFortroligAdresse",
                    grupper = emptySet(),
                    tilganger = setOf(TilgangType.SAKSBEHANDLER, TilgangType.BESLUTTER, TilgangType.FORTROLIG_ADRESSE),
                )
            val beslutterStrengtFortroligAdresse =
                Saksbehandler(
                    navIdent = "beslutterStrengtFortroligAdresse",
                    grupper = emptySet(),
                    tilganger =
                        setOf(
                            TilgangType.SAKSBEHANDLER,
                            TilgangType.BESLUTTER,
                            TilgangType.STRENGT_FORTROLIG_ADRESSE,
                        ),
                )
            val beslutterStrengtFortroligAdresseUtland =
                Saksbehandler(
                    navIdent = "beslutterStrengtFortroligAdresseUtland",
                    grupper = emptySet(),
                    tilganger =
                        setOf(
                            TilgangType.SAKSBEHANDLER,
                            TilgangType.BESLUTTER,
                            TilgangType.STRENGT_FORTROLIG_ADRESSE_UTLAND,
                        ),
                )
            val beslutterStrengtFortroligOgEgneAnsatte =
                Saksbehandler(
                    navIdent = "beslutterStrengtFortroligOgEgneAnsatte",
                    grupper = emptySet(),
                    tilganger =
                        setOf(
                            TilgangType.SAKSBEHANDLER,
                            TilgangType.BESLUTTER,
                            TilgangType.STRENGT_FORTROLIG_ADRESSE,
                            TilgangType.EGNE_ANSATTE,
                        ),
                )

            val yngsteLedigeOppgaveOpprettetIDag =
                this.leggTilOppgave(
                    tilstand = Oppgave.KlarTilBehandling,
                    opprettet = opprettetNå,
                )

            val oppgaveMedEmneknagg =
                this.leggTilOppgave(
                    tilstand = Oppgave.KlarTilBehandling,
                    opprettet = opprettetNå.minusDays(5),
                    emneknagger = setOf("Testknagg"),
                )

            val eldsteLedigeOppgaveKlarTilBehandling =
                this.leggTilOppgave(
                    tilstand = Oppgave.KlarTilBehandling,
                    opprettet = opprettetNå.minusDays(10),
                )

            this.leggTilOppgave(
                tilstand = Oppgave.KlarTilBehandling,
                opprettet = opprettetNå.minusDays(11),
                saksbehandlerIdent = saksbehandler.navIdent,
            )

            this.leggTilOppgave(
                tilstand = Oppgave.FerdigBehandlet,
                opprettet = opprettetNå.minusDays(12),
                saksbehandlerIdent = testSaksbehandler.navIdent,
                tilstandslogg = tilstandsloggUnderBehandling(),
            )

            this.leggTilOppgave(
                tilstand = Oppgave.Opprettet,
                opprettet = opprettetNå.minusDays(13),
            )

            val eldsteKontrollOppgaveUtenSkjermingOgAdressegradering =
                this.leggTilOppgave(
                    tilstand = Oppgave.KlarTilKontroll,
                    opprettet = opprettetNå.minusDays(14),
                    tilstandslogg = tilstandsloggUnderBehandling(),
                )

            val eldsteKontrollOppgaveEgneAnsatteSkjerming =
                this.leggTilOppgave(
                    tilstand = Oppgave.KlarTilKontroll,
                    opprettet = opprettetNå.minusDays(15),
                    person = lagPerson(skjermesSomEgneAnsatte = true),
                    tilstandslogg = tilstandsloggUnderBehandling(),
                )

            val eldsteKontrollOppgaveFortroligAdresse =
                this.leggTilOppgave(
                    tilstand = Oppgave.KlarTilKontroll,
                    opprettet = opprettetNå.minusDays(16),
                    person = lagPerson(addresseBeskyttelseGradering = FORTROLIG),
                    tilstandslogg = tilstandsloggUnderBehandling(),
                )

            val eldsteKontrollOppgaveStrengtFortroligAdresse =
                this.leggTilOppgave(
                    tilstand = Oppgave.KlarTilKontroll,
                    opprettet = opprettetNå.minusDays(17),
                    person = lagPerson(addresseBeskyttelseGradering = STRENGT_FORTROLIG),
                    tilstandslogg = tilstandsloggUnderBehandling(),
                )

            val eldsteKontrollOppgaveStrengtFortroligAdresseUtland =
                this.leggTilOppgave(
                    tilstand = Oppgave.KlarTilKontroll,
                    opprettet = opprettetNå.minusDays(18),
                    person = lagPerson(addresseBeskyttelseGradering = AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND),
                    tilstandslogg = tilstandsloggUnderBehandling(),
                )

            val eldsteKontrollOppgaveStrengtFortroligAdresseOgEgneAnsatteSkjerming =
                this.leggTilOppgave(
                    tilstand = Oppgave.KlarTilKontroll,
                    opprettet = opprettetNå.minusDays(19),
                    person =
                        lagPerson(
                            skjermesSomEgneAnsatte = true,
                            addresseBeskyttelseGradering = STRENGT_FORTROLIG,
                        ),
                    tilstandslogg = tilstandsloggUnderBehandling(),
                )

            val emneknaggFilterForTestSaksbehandler =
                TildelNesteOppgaveFilter(
                    periode = Periode.Companion.UBEGRENSET_PERIODE,
                    emneknagger = setOf("Testknagg"),
                    adressebeskyttelseTilganger = setOf(UGRADERT),
                    navIdent = testSaksbehandler.navIdent,
                )
            val opprettetIDagFilterForTestSaksbehandler =
                TildelNesteOppgaveFilter(
                    periode = Periode(fom = opprettetNå.toLocalDate(), tom = opprettetNå.toLocalDate()),
                    emneknagger = emptySet(),
                    adressebeskyttelseTilganger = setOf(UGRADERT),
                    navIdent = testSaksbehandler.navIdent,
                )

            val repo = PostgresOppgaveRepository(ds)
            repo.tildelOgHentNesteOppgave(
                nesteOppgaveHendelse =
                    NesteOppgaveHendelse(
                        ansvarligIdent = testSaksbehandler.navIdent,
                        utførtAv = testSaksbehandler,
                    ),
                filter = emneknaggFilterForTestSaksbehandler,
            ).let {
                assertSoftly {
                    require(it != null) { "Skal finne en oppgave" }
                    it.oppgaveId shouldBe oppgaveMedEmneknagg.oppgaveId
                    it.behandlerIdent shouldBe testSaksbehandler.navIdent
                    it.tilstand() shouldBe Oppgave.UnderBehandling
                }
            }

            repo.tildelOgHentNesteOppgave(
                nesteOppgaveHendelse =
                    NesteOppgaveHendelse(
                        ansvarligIdent = testSaksbehandler.navIdent,
                        utførtAv = testSaksbehandler,
                    ),
                filter = opprettetIDagFilterForTestSaksbehandler,
            ).let {
                assertSoftly {
                    require(it != null) { "Skal finne en oppgave" }
                    it.oppgaveId shouldBe yngsteLedigeOppgaveOpprettetIDag.oppgaveId
                    it.behandlerIdent shouldBe testSaksbehandler.navIdent
                    it.tilstand() shouldBe Oppgave.UnderBehandling
                }
            }

            // Skal ikke hente beslutter-oppgaver
            repo.tildelOgHentNesteOppgave(
                nesteOppgaveHendelse =
                    NesteOppgaveHendelse(
                        ansvarligIdent = testSaksbehandler.navIdent,
                        utførtAv = testSaksbehandler,
                    ),
                filter =
                    TildelNesteOppgaveFilter(
                        periode = Periode.Companion.UBEGRENSET_PERIODE,
                        emneknagger = emptySet(),
                        egneAnsatteTilgang = testSaksbehandler.tilganger.contains(TilgangType.EGNE_ANSATTE),
                        adressebeskyttelseTilganger = testSaksbehandler.adressebeskyttelseTilganger(),
                        harBeslutterRolle = testSaksbehandler.tilganger.contains(TilgangType.BESLUTTER),
                        navIdent = testSaksbehandler.navIdent,
                    ),
            ).let {
                assertSoftly {
                    require(it != null) { "Skal finne en oppgave" }
                    it.oppgaveId shouldBe eldsteLedigeOppgaveKlarTilBehandling.oppgaveId
                    it.behandlerIdent shouldBe testSaksbehandler.navIdent
                    it.tilstand() shouldBe Oppgave.UnderBehandling
                }
            }

            repo.tildelOgHentNesteOppgave(
                nesteOppgaveHendelse =
                    NesteOppgaveHendelse(
                        ansvarligIdent = vanligBeslutter.navIdent,
                        utførtAv = vanligBeslutter,
                    ),
                filter =
                    TildelNesteOppgaveFilter(
                        periode = Periode.Companion.UBEGRENSET_PERIODE,
                        emneknagger = emptySet(),
                        egneAnsatteTilgang = beslutter.tilganger.contains(TilgangType.EGNE_ANSATTE),
                        adressebeskyttelseTilganger = beslutter.adressebeskyttelseTilganger(),
                        harBeslutterRolle = beslutter.tilganger.contains(TilgangType.BESLUTTER),
                        navIdent = beslutter.navIdent,
                    ),
            ).let {
                assertSoftly {
                    require(it != null) { "Skal finne en oppgave" }
                    it.oppgaveId shouldBe eldsteKontrollOppgaveUtenSkjermingOgAdressegradering.oppgaveId
                    it.tilstand().type shouldBe Oppgave.Tilstand.Type.UNDER_KONTROLL
                    it.sisteBeslutter() shouldBe vanligBeslutter.navIdent
                }
            }

            repo.tildelOgHentNesteOppgave(
                nesteOppgaveHendelse =
                    NesteOppgaveHendelse(
                        ansvarligIdent = beslutterEgneAnsatte.navIdent,
                        utførtAv = beslutterEgneAnsatte,
                    ),
                filter =
                    TildelNesteOppgaveFilter(
                        periode = Periode.Companion.UBEGRENSET_PERIODE,
                        emneknagger = emptySet(),
                        egneAnsatteTilgang = beslutterEgneAnsatte.tilganger.contains(TilgangType.EGNE_ANSATTE),
                        adressebeskyttelseTilganger = beslutterEgneAnsatte.adressebeskyttelseTilganger(),
                        harBeslutterRolle = beslutterEgneAnsatte.tilganger.contains(TilgangType.BESLUTTER),
                        navIdent = beslutterEgneAnsatte.navIdent,
                    ),
            ).let {
                assertSoftly {
                    require(it != null) { "Skal finne en oppgave" }
                    it.oppgaveId shouldBe eldsteKontrollOppgaveEgneAnsatteSkjerming.oppgaveId
                    it.tilstand().type shouldBe Oppgave.Tilstand.Type.UNDER_KONTROLL
                    it.sisteBeslutter() shouldBe beslutterEgneAnsatte.navIdent
                }
            }

            repo.tildelOgHentNesteOppgave(
                nesteOppgaveHendelse =
                    NesteOppgaveHendelse(
                        ansvarligIdent = beslutterFortroligAdresse.navIdent,
                        utførtAv = beslutterFortroligAdresse,
                    ),
                filter =
                    TildelNesteOppgaveFilter(
                        periode = Periode.Companion.UBEGRENSET_PERIODE,
                        emneknagger = emptySet(),
                        egneAnsatteTilgang = beslutterFortroligAdresse.tilganger.contains(TilgangType.EGNE_ANSATTE),
                        adressebeskyttelseTilganger = beslutterFortroligAdresse.adressebeskyttelseTilganger(),
                        harBeslutterRolle = beslutterFortroligAdresse.tilganger.contains(TilgangType.BESLUTTER),
                        navIdent = beslutterFortroligAdresse.navIdent,
                    ),
            ).let {
                assertSoftly {
                    require(it != null) { "Skal finne en oppgave" }
                    it.oppgaveId shouldBe eldsteKontrollOppgaveFortroligAdresse.oppgaveId
                    it.tilstand().type shouldBe Oppgave.Tilstand.Type.UNDER_KONTROLL
                    it.sisteBeslutter() shouldBe beslutterFortroligAdresse.navIdent
                }
            }

            repo.tildelOgHentNesteOppgave(
                nesteOppgaveHendelse =
                    NesteOppgaveHendelse(
                        ansvarligIdent = beslutterStrengtFortroligAdresse.navIdent,
                        utførtAv = beslutterStrengtFortroligAdresse,
                    ),
                filter =
                    TildelNesteOppgaveFilter(
                        periode = Periode.Companion.UBEGRENSET_PERIODE,
                        emneknagger = emptySet(),
                        egneAnsatteTilgang = beslutterStrengtFortroligAdresse.tilganger.contains(TilgangType.EGNE_ANSATTE),
                        adressebeskyttelseTilganger = beslutterStrengtFortroligAdresse.adressebeskyttelseTilganger(),
                        harBeslutterRolle = beslutterStrengtFortroligAdresse.tilganger.contains(TilgangType.BESLUTTER),
                        navIdent = beslutterStrengtFortroligAdresse.navIdent,
                    ),
            ).let {
                assertSoftly {
                    require(it != null) { "Skal finne en oppgave" }
                    it.oppgaveId shouldBe eldsteKontrollOppgaveStrengtFortroligAdresse.oppgaveId
                    it.tilstand().type shouldBe Oppgave.Tilstand.Type.UNDER_KONTROLL
                    it.sisteBeslutter() shouldBe beslutterStrengtFortroligAdresse.navIdent
                    it.opprettet shouldBe eldsteKontrollOppgaveStrengtFortroligAdresse.opprettet
                }
            }

            repo.tildelOgHentNesteOppgave(
                nesteOppgaveHendelse =
                    NesteOppgaveHendelse(
                        ansvarligIdent = beslutterStrengtFortroligAdresseUtland.navIdent,
                        utførtAv = beslutterStrengtFortroligAdresseUtland,
                    ),
                filter =
                    TildelNesteOppgaveFilter(
                        periode = Periode.Companion.UBEGRENSET_PERIODE,
                        emneknagger = emptySet(),
                        egneAnsatteTilgang = beslutterStrengtFortroligAdresseUtland.tilganger.contains(TilgangType.EGNE_ANSATTE),
                        adressebeskyttelseTilganger = beslutterStrengtFortroligAdresseUtland.adressebeskyttelseTilganger(),
                        harBeslutterRolle = beslutterStrengtFortroligAdresseUtland.tilganger.contains(TilgangType.BESLUTTER),
                        navIdent = beslutterStrengtFortroligAdresseUtland.navIdent,
                    ),
            ).let {
                assertSoftly {
                    require(it != null) { "Skal finne en oppgave" }
                    it.oppgaveId shouldBe eldsteKontrollOppgaveStrengtFortroligAdresseUtland.oppgaveId
                    it.tilstand().type shouldBe Oppgave.Tilstand.Type.UNDER_KONTROLL
                    it.sisteBeslutter() shouldBe beslutterStrengtFortroligAdresseUtland.navIdent
                }
            }

            repo.tildelOgHentNesteOppgave(
                nesteOppgaveHendelse =
                    NesteOppgaveHendelse(
                        ansvarligIdent = beslutterStrengtFortroligOgEgneAnsatte.navIdent,
                        utførtAv = beslutterStrengtFortroligOgEgneAnsatte,
                    ),
                filter =
                    TildelNesteOppgaveFilter(
                        periode = Periode.Companion.UBEGRENSET_PERIODE,
                        emneknagger = emptySet(),
                        egneAnsatteTilgang =
                            beslutterStrengtFortroligOgEgneAnsatte.tilganger.contains(
                                TilgangType.EGNE_ANSATTE,
                            ),
                        adressebeskyttelseTilganger = beslutterStrengtFortroligOgEgneAnsatte.adressebeskyttelseTilganger(),
                        harBeslutterRolle =
                            beslutterStrengtFortroligOgEgneAnsatte.tilganger.contains(
                                TilgangType.BESLUTTER,
                            ),
                        navIdent = beslutterStrengtFortroligOgEgneAnsatte.navIdent,
                    ),
            ).let {
                assertSoftly {
                    require(it != null) { "Skal finne en oppgave" }
                    it.oppgaveId shouldBe eldsteKontrollOppgaveStrengtFortroligAdresseOgEgneAnsatteSkjerming.oppgaveId
                    it.tilstand().type shouldBe Oppgave.Tilstand.Type.UNDER_KONTROLL
                    it.sisteBeslutter() shouldBe beslutterStrengtFortroligOgEgneAnsatte.navIdent
                }
            }
        }
    }

    @Test
    fun `Skal kunne lagre en oppgave flere ganger`() {
        val testOppgave = lagOppgave()

        DBTestHelper.Companion.withOppgave(oppgave = testOppgave) { ds ->
            val repo = PostgresOppgaveRepository(ds)
            shouldNotThrowAny {
                repo.lagre(testOppgave)
                repo.lagre(testOppgave)
            }
        }
    }

    @Test
    fun `Skal kunne lagre og hente en oppgave med notat`() {
        val testOppgave = lagOppgave(tilstand = Oppgave.KlarTilKontroll)
        DBTestHelper.Companion.withOppgave(testOppgave) { ds ->
            testOppgave.tildel(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = testOppgave.oppgaveId,
                    ansvarligIdent = beslutter.navIdent,
                    utførtAv = beslutter,
                ),
            )
            testOppgave.lagreNotat(
                NotatHendelse(
                    oppgaveId = testOppgave.oppgaveId,
                    tekst = "Dette er et notat",
                    utførtAv = beslutter,
                ),
            )
            val repo = PostgresOppgaveRepository(ds)
            repo.lagre(testOppgave)
            val oppgaveFraDatabase = repo.hentOppgave(testOppgave.oppgaveId)

            oppgaveFraDatabase shouldBe testOppgave
        }
    }

    @Test
    fun `Skal kunne lagre notatet til en oppgave`() {
        val oppgave = lagOppgave(tilstand = Oppgave.KlarTilKontroll)
        DBTestHelper.Companion.withOppgave(oppgave) { ds ->

            val repo = PostgresOppgaveRepository(ds)
            oppgave.tildel(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    beslutter.navIdent,
                    beslutter,
                ),
            )
            repo.lagre(oppgave)
            repo.hentOppgave(oppgave.oppgaveId).tilstand().notat() shouldBe null

            oppgave.lagreNotat(
                NotatHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    tekst = "Dette er et notat",
                    utførtAv = beslutter,
                ),
            )
            repo.lagreNotatFor(oppgave)
            repo.finnNotat(oppgave.tilstandslogg.first().id)?.hentTekst().let {
                it shouldBe "Dette er et notat"
            }
        }
    }

    @Test
    fun `Skal kunne finne et notat`() {
        val oppgave = lagOppgave(tilstand = Oppgave.KlarTilKontroll)
        DBTestHelper.Companion.withOppgave(oppgave) { ds ->

            val repo = PostgresOppgaveRepository(ds)
            oppgave.tildel(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    beslutter.navIdent,
                    beslutter,
                ),
            )
            oppgave.lagreNotat(
                NotatHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    tekst = "Dette er et notat",
                    utførtAv = beslutter,
                ),
            )
            repo.lagre(oppgave)

            repo.finnNotat(oppgave.tilstandslogg.first().id)?.hentTekst().let {
                it shouldBe "Dette er et notat"
            }

            repo.finnNotat(UUIDv7.ny()) shouldBe null
        }
    }

    @Test
    fun `Skal kunne lagre og hente en oppgave`() {
        val behandling = lagBehandling()
        val testOppgave = lagOppgave(behandlingId = behandling.behandlingId, person = testPerson)

        DBTestHelper.Companion.withBehandling(behandling = behandling, person = testPerson) { ds ->
            val repo = PostgresOppgaveRepository(ds)
            repo.lagre(testOppgave)
            val oppgaveFraDatabase = repo.hentOppgave(testOppgave.oppgaveId)
            oppgaveFraDatabase shouldBe testOppgave
        }
    }

    @Test
    fun `Skal kunne slette et notat for en oppgave`() {
        val testOppgave = lagOppgave(tilstand = Oppgave.KlarTilKontroll)
        DBTestHelper.Companion.withOppgave(testOppgave) { ds ->

            testOppgave.tildel(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = testOppgave.oppgaveId,
                    ansvarligIdent = beslutter.navIdent,
                    utførtAv = beslutter,
                ),
            )
            testOppgave.lagreNotat(
                NotatHendelse(
                    oppgaveId = testOppgave.oppgaveId,
                    tekst = "Dette er et notat",
                    utførtAv = beslutter,
                ),
            )
            val repo = PostgresOppgaveRepository(ds)
            repo.lagre(testOppgave)

            val oppgaveFraDatabase = repo.hentOppgave(testOppgave.oppgaveId)

            oppgaveFraDatabase shouldBe testOppgave
            oppgaveFraDatabase.tilstand().notat()!!.hentTekst() shouldBe "Dette er et notat"

            repo.slettNotatFor(oppgaveFraDatabase)
            repo.hentOppgave(testOppgave.oppgaveId).tilstand().notat() shouldBe null

            repo.slettNotatFor(oppgaveFraDatabase)
        }
    }

    @Test
    fun `Skal kunne lagre og hente tilstandslogg for en spesifikk oppgave`() {
        val nå = LocalDateTime.now()
        val tilstandslogg =
            OppgaveTilstandslogg(
                Tilstandsendring(
                    tilstand = Oppgave.Tilstand.Type.KLAR_TIL_KONTROLL,
                    hendelse =
                        SendTilKontrollHendelse(
                            oppgaveId = oppgaveIdTest,
                            utførtAv = saksbehandler,
                        ),
                    tidspunkt = nå.minusDays(2).truncatedTo(ChronoUnit.SECONDS),
                ),
                Tilstandsendring(
                    tilstand = Oppgave.Tilstand.Type.UNDER_KONTROLL,
                    hendelse =
                        SettOppgaveAnsvarHendelse(
                            oppgaveId = oppgaveIdTest,
                            beslutter.navIdent,
                            beslutter,
                        ),
                    tidspunkt = nå.minusDays(1).truncatedTo(ChronoUnit.SECONDS),
                ),
                Tilstandsendring(
                    tilstand = Oppgave.Tilstand.Type.FERDIG_BEHANDLET,
                    hendelse =
                        GodkjentBehandlingHendelse(
                            oppgaveId = oppgaveIdTest,
                            meldingOmVedtak = "<h1>This is HTML</h1>",
                            utførtAv = beslutter,
                        ),
                    tidspunkt = nå.truncatedTo(ChronoUnit.SECONDS),
                ),
            )
        val testOppgave = lagOppgave(tilstandslogg = tilstandslogg, oppgaveId = oppgaveIdTest)
        DBTestHelper.Companion.withOppgave(testOppgave) { ds ->

            val repo = PostgresOppgaveRepository(ds)
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
        val testOppgave = lagOppgave(tilstand = Oppgave.KlarTilBehandling)
        DBTestHelper.Companion.withOppgave(testOppgave) { ds ->
            val repo = PostgresOppgaveRepository(ds)

            repo.lagre(testOppgave)
            repo.hentOppgave(testOppgave.oppgaveId).tilstand().type shouldBe Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING

            repo.lagre(testOppgave.copy(tilstand = Oppgave.FerdigBehandlet))
            repo.hentOppgave(testOppgave.oppgaveId).tilstand().type shouldBe Oppgave.Tilstand.Type.FERDIG_BEHANDLET
        }
    }

    @Test
    fun `CRUD på oppgave i tilstand PAA_VENT`() {
        val testOppgave = lagOppgave(tilstand = Oppgave.UnderBehandling)
        val utsattTil = LocalDate.now().plusDays(1)
        DBTestHelper.Companion.withOppgave(testOppgave) { ds ->
            val repo = PostgresOppgaveRepository(ds)
            repo.lagre(
                testOppgave.copy(
                    tilstand = Oppgave.PåVent,
                    utsattTil = utsattTil,
                ),
            )
            repo.hentOppgave(testOppgave.oppgaveId).let { oppgave: Oppgave ->
                oppgave.tilstand() shouldBe Oppgave.PåVent
                oppgave.utsattTil() shouldBe utsattTil
            }
        }
    }

    @Test
    fun `Skal kunne søke etter oppgaver filtrert på tilstand`() {
        DBTestHelper.Companion.withMigratedDb { ds ->
            val oppgaveKlarTilBehandling = this.leggTilOppgave(tilstand = Oppgave.KlarTilBehandling)
            val oppgaveFerdigBehandlet = this.leggTilOppgave(tilstand = Oppgave.FerdigBehandlet)
            val repo = PostgresOppgaveRepository(ds)
            repo.hentAlleOppgaverMedTilstand(Oppgave.Tilstand.Type.FERDIG_BEHANDLET).let { oppgaver ->
                oppgaver.size shouldBe 1
                oppgaver.single().oppgaveId shouldBe oppgaveFerdigBehandlet.oppgaveId
            }

            repo.hentAlleOppgaverMedTilstand(Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING).let { oppgaver ->
                oppgaver.size shouldBe 1
                oppgaver.single().oppgaveId shouldBe oppgaveKlarTilBehandling.oppgaveId
            }
        }
    }

    @Test
    fun `Skal kunne søke etter oppgaver filtrert på type utløsende hendelse`() {
        DBTestHelper.Companion.withMigratedDb { ds ->
            this.leggTilOppgave(
                tilstand = Oppgave.KlarTilBehandling,
                type = UtløstAvType.SØKNAD,
                opprettet = opprettetNå,
            )
            val klageOppgave =
                this.leggTilOppgave(
                    tilstand = Oppgave.KlarTilBehandling,
                    type = UtløstAvType.KLAGE,
                    opprettet = opprettetNå,
                )

            val repo = PostgresOppgaveRepository(ds)
            repo.søk(
                søkeFilter =
                    Søkefilter(
                        tilstander = Oppgave.Tilstand.Type.entries.toSet(),
                        periode = Periode.Companion.UBEGRENSET_PERIODE,
                        emneknagger = emptySet(),
                        utløstAvTyper = setOf(UtløstAvType.KLAGE),
                    ),
            ).oppgaver shouldBe listOf(klageOppgave)
        }
    }

    @Test
    fun `Skal kunne hente alle oppgaver for en gitt person, sortert på opprettet`() {
        val ola =
            Person(
                ident = "12345678910",
                skjermesSomEgneAnsatte = false,
                adressebeskyttelseGradering = UGRADERT,
            )
        val gry =
            Person(
                ident = "10987654321",
                skjermesSomEgneAnsatte = false,
                adressebeskyttelseGradering = UGRADERT,
            )

        DBTestHelper.Companion.withMigratedDb { ds ->
            val repo = PostgresOppgaveRepository(ds)
            val oppgave1TilOla =
                this.leggTilOppgave(person = ola, tilstand = Oppgave.KlarTilBehandling, opprettet = opprettetNå)
            val oppgave2TilOla =
                this.leggTilOppgave(
                    person = ola,
                    tilstand = Oppgave.FerdigBehandlet,
                    opprettet = opprettetNå.minusDays(1),
                )
            val oppgave1TilGry =
                this.leggTilOppgave(
                    person = gry,
                    tilstand = Oppgave.FerdigBehandlet,
                    opprettet = opprettetNå.minusDays(2),
                )

            repo.finnOppgaverFor(ola.ident) shouldBe listOf(oppgave2TilOla, oppgave1TilOla)
            repo.finnOppgaverFor(gry.ident) shouldBe listOf(oppgave1TilGry)
        }
    }

    @Test
    fun `Skal kunne hente oppgavetilstand for søknad`() {
        val hendelse =
            SøknadsbehandlingOpprettetHendelse(
                søknadId = UUIDv7.ny(),
                behandlingId = UUIDv7.ny(),
                ident = "12345678910",
                opprettet = LocalDateTime.now(),
            )
        val person =
            Person(
                id = UUIDv7.ny(),
                ident = hendelse.ident,
                skjermesSomEgneAnsatte = false,
                adressebeskyttelseGradering = UGRADERT,
            )

        val behandling =
            Behandling(
                behandlingId = hendelse.behandlingId,
                opprettet = hendelse.opprettet,
                hendelse = hendelse,
                utløstAv = UtløstAvType.SØKNAD,
            )

        DBTestHelper.Companion.withBehandling(
            person = person,
            sak =
                Sak(
                    sakId = UUIDv7.ny(),
                    søknadId = hendelse.søknadId,
                    opprettet = hendelse.opprettet,
                    behandlinger = mutableSetOf(behandling),
                ),
        ) { ds ->
            val oppgve =
                lagOppgave(
                    oppgaveId = UUIDv7.ny(),
                    behandlingId = hendelse.behandlingId,
                    person = person,
                )
            val repo = PostgresOppgaveRepository(ds)
            repo.lagre(oppgave = oppgve)

            repo.oppgaveTilstandForSøknad(
                ident = hendelse.ident,
                søknadId = hendelse.søknadId,
            ) shouldBe Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING

            repo.oppgaveTilstandForSøknad(
                ident = hendelse.ident,
                søknadId = UUIDv7.ny(),
            ) shouldBe null
        }
    }

    @Test
    fun `Skal hente oppgaveId fra behandlingId`() {
        val behandlingId = UUIDv7.ny()
        val behandling = lagBehandling(behandlingId = behandlingId)
        val oppgave = lagOppgave(behandlingId = behandlingId, utløstAvType = UtløstAvType.SØKNAD)

        DBTestHelper.Companion.withBehandling(behandling = behandling) { ds ->
            val repo = PostgresOppgaveRepository(ds)
            repo.lagre(oppgave)
            repo.hentOppgaveIdFor(behandlingId = behandlingId) shouldBe oppgave.oppgaveId
            repo.hentOppgaveIdFor(behandlingId = UUIDv7.ny()) shouldBe null
        }
    }

    @Test
    fun `Skal kunne søke etter oppgaver filtrert på emneknagger`() {
        DBTestHelper.Companion.withMigratedDb { ds ->
            val oppgave1 = this.leggTilOppgave(emneknagger = setOf("hubba", "bubba"), opprettet = opprettetNå)
            val oppgave2 = this.leggTilOppgave(emneknagger = setOf("hubba"), opprettet = opprettetNå)
            val oppgave3 = this.leggTilOppgave(emneknagger = emptySet(), opprettet = opprettetNå)

            val repo = PostgresOppgaveRepository(ds)
            repo.søk(
                Søkefilter(
                    tilstander = Oppgave.Tilstand.Type.entries.toSet(),
                    periode = Periode.Companion.UBEGRENSET_PERIODE,
                    emneknagger = emptySet(),
                ),
            ).oppgaver shouldBe listOf(oppgave1, oppgave2, oppgave3)

            repo.søk(
                Søkefilter(
                    tilstander = Oppgave.Tilstand.Type.entries.toSet(),
                    periode = Periode.Companion.UBEGRENSET_PERIODE,
                    emneknagger = setOf("hubba"),
                ),
            ).oppgaver shouldBe listOf(oppgave1, oppgave2)

            repo.søk(
                Søkefilter(
                    tilstander = Oppgave.Tilstand.Type.entries.toSet(),
                    periode = Periode.Companion.UBEGRENSET_PERIODE,
                    emneknagger = setOf("bubba"),
                ),
            ).oppgaver shouldBe listOf(oppgave1)

            repo.søk(
                Søkefilter(
                    tilstander = Oppgave.Tilstand.Type.entries.toSet(),
                    periode = Periode.Companion.UBEGRENSET_PERIODE,
                    emneknagger = setOf("bubba", "hubba"),
                ),
            ).oppgaver shouldBe listOf(oppgave1, oppgave2)
        }
    }

    @Test
    fun `Skal kunne søke etter oppgaver tildel en gitt saksbehandler`() {
        val enUkeSiden = opprettetNå.minusDays(7)
        val saksbehandler1 = "saksbehandler1"
        val saksbehandler2 = "saksbehandler2"

        DBTestHelper.Companion.withMigratedDb { ds ->
            this.leggTilOppgave(
                tilstand = Oppgave.UnderBehandling,
                opprettet = enUkeSiden,
                saksbehandlerIdent = saksbehandler1,
                emneknagger = setOf(Emneknagg.Regelknagg.INNVILGELSE.visningsnavn),
            )
            this.leggTilOppgave(
                tilstand = Oppgave.UnderBehandling,
                saksbehandlerIdent = saksbehandler2,
                emneknagger = setOf(Emneknagg.Regelknagg.AVSLAG_MINSTEINNTEKT.visningsnavn),
            )
            this.leggTilOppgave(
                tilstand = Oppgave.FerdigBehandlet,
                saksbehandlerIdent = saksbehandler2,
                emneknagger = setOf(Emneknagg.Regelknagg.INNVILGELSE.visningsnavn),
            )
            this.leggTilOppgave(
                tilstand = Oppgave.UnderBehandling,
                saksbehandlerIdent = null,
                emneknagger = setOf(Emneknagg.Regelknagg.INNVILGELSE.visningsnavn),
            )

            val repo = PostgresOppgaveRepository(ds)
            repo.søk(
                Søkefilter(
                    tilstander = Oppgave.Tilstand.Type.entries.toSet(),
                    periode = Periode.Companion.UBEGRENSET_PERIODE,
                    saksbehandlerIdent = saksbehandler1,
                ),
            ).oppgaver.size shouldBe 1

            repo.søk(
                Søkefilter(
                    tilstander = Oppgave.Tilstand.Type.entries.toSet(),
                    periode = Periode.Companion.UBEGRENSET_PERIODE,
                    saksbehandlerIdent = saksbehandler2,
                ),
            ).oppgaver.size shouldBe 2

            repo.søk(
                Søkefilter(
                    tilstander = Oppgave.Tilstand.Type.entries.toSet(),
                    periode = Periode.Companion.UBEGRENSET_PERIODE,
                    saksbehandlerIdent = null,
                ),
            ).oppgaver.size shouldBe 4

            repo.søk(
                Søkefilter(
                    tilstander = Oppgave.Tilstand.Type.entries.toSet(),
                    periode = Periode.Companion.UBEGRENSET_PERIODE,
                    saksbehandlerIdent = saksbehandler2,
                    emneknagger = setOf(Emneknagg.Regelknagg.INNVILGELSE.visningsnavn),
                ),
            ).oppgaver.size shouldBe 1
        }
    }

    @Test
    fun `Skal kunne hente paginerte oppgaver`() {
        DBTestHelper.Companion.withMigratedDb { ds ->
            val nyesteOppgave = this.leggTilOppgave(opprettet = opprettetNå)
            val nestNyesteOppgave = this.leggTilOppgave(opprettet = opprettetNå.minusDays(1))
            val nestEldsteOppgave = this.leggTilOppgave(opprettet = opprettetNå.minusDays(3))
            val eldsteOppgave = this.leggTilOppgave(opprettet = opprettetNå.minusDays(7))

            val repo = PostgresOppgaveRepository(ds)
            repo.søk(
                Søkefilter(
                    tilstander = Oppgave.Tilstand.Type.Companion.søkbareTilstander,
                    periode = Periode.Companion.UBEGRENSET_PERIODE,
                    paginering = null,
                ),
            ).let {
                it.oppgaver.size shouldBe 4
                it.totaltAntallOppgaver shouldBe 4
            }

            repo.søk(
                Søkefilter(
                    tilstander = Oppgave.Tilstand.Type.Companion.søkbareTilstander,
                    periode = Periode.Companion.UBEGRENSET_PERIODE,
                    paginering = Søkefilter.Paginering(2, 0),
                ),
            ).let {
                it.oppgaver.size shouldBe 2
                it.oppgaver[0] shouldBe eldsteOppgave
                it.oppgaver[1] shouldBe nestEldsteOppgave
                it.totaltAntallOppgaver shouldBe 4
            }

            repo.søk(
                Søkefilter(
                    tilstander = Oppgave.Tilstand.Type.entries.toSet(),
                    periode = Periode.Companion.UBEGRENSET_PERIODE,
                    paginering = Søkefilter.Paginering(2, 1),
                ),
            ).let {
                it.oppgaver.size shouldBe 2
                it.oppgaver[0] shouldBe nestNyesteOppgave
                it.oppgaver[1] shouldBe nyesteOppgave
                it.totaltAntallOppgaver shouldBe 4
            }

            repo.søk(
                Søkefilter(
                    tilstander = Oppgave.Tilstand.Type.Companion.søkbareTilstander,
                    periode = Periode.Companion.UBEGRENSET_PERIODE,
                    paginering = Søkefilter.Paginering(10, 0),
                ),
            ).let {
                it.oppgaver.size shouldBe 4
                it.oppgaver[0] shouldBe eldsteOppgave
                it.oppgaver[1] shouldBe nestEldsteOppgave
                it.oppgaver[2] shouldBe nestNyesteOppgave
                it.oppgaver[3] shouldBe nyesteOppgave
                it.totaltAntallOppgaver shouldBe 4
            }
            repo.søk(
                Søkefilter(
                    tilstander = Oppgave.Tilstand.Type.Companion.søkbareTilstander,
                    periode = Periode.Companion.UBEGRENSET_PERIODE,
                    paginering = Søkefilter.Paginering(10, 1),
                ),
            ).let {
                it.oppgaver.size shouldBe 0
                it.totaltAntallOppgaver shouldBe 4
            }
        }
    }

    @Test
    fun `Skal kunne søke etter oppgaver filtrert på tilstand og opprettet`() {
        val enUkeSiden = opprettetNå.minusDays(7)

        DBTestHelper.Companion.withMigratedDb { ds ->
            val oppgaveUnderBehandlingEnUkeGammel =
                this.leggTilOppgave(
                    tilstand = Oppgave.UnderBehandling,
                    opprettet = enUkeSiden,
                    saksbehandlerIdent = saksbehandler.navIdent,
                )
            this.leggTilOppgave(
                tilstand = Oppgave.KlarTilBehandling,
                opprettet = opprettetNå,
            )
            this.leggTilOppgave(
                tilstand = Oppgave.KlarTilBehandling,
                opprettet = opprettetNå.minusDays(1),
            )

            val repo = PostgresOppgaveRepository(ds)
            repo.søk(
                Søkefilter(
                    tilstander = setOf(Oppgave.Tilstand.Type.UNDER_BEHANDLING),
                    periode = Periode.Companion.UBEGRENSET_PERIODE,
                ),
            ).oppgaver.single() shouldBe oppgaveUnderBehandlingEnUkeGammel

            repo.søk(
                Søkefilter(
                    tilstander =
                        setOf(
                            Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING,
                            Oppgave.Tilstand.Type.UNDER_BEHANDLING,
                        ),
                    periode = Periode.Companion.UBEGRENSET_PERIODE,
                ),
            ).oppgaver.size shouldBe 3

            repo.søk(
                Søkefilter(
                    periode = Periode.Companion.UBEGRENSET_PERIODE,
                    tilstander = Oppgave.Tilstand.Type.Companion.søkbareTilstander,
                    saksbehandlerIdent = null,
                    personIdent = null,
                    oppgaveId = null,
                    behandlingId = null,
                ),
            ).let {
                it.oppgaver.size shouldBe 3
                it.oppgaver.map { oppgave -> oppgave.tilstand().type }.toSet() shouldBe
                    setOf(
                        Oppgave.Tilstand.Type.UNDER_BEHANDLING,
                        Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING,
                    )
            }

            repo.søk(
                Søkefilter(
                    tilstander = setOf(Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING),
                    periode =
                        Periode(
                            fom = enUkeSiden.plusDays(1).toLocalDate(),
                            tom = enUkeSiden.plusDays(2).toLocalDate(),
                        ),
                ),
            ).oppgaver.size shouldBe 0

            repo.søk(
                Søkefilter(
                    tilstander = setOf(Oppgave.Tilstand.Type.UNDER_BEHANDLING),
                    periode =
                        Periode(
                            fom = enUkeSiden.minusDays(1).toLocalDate(),
                            tom = enUkeSiden.plusDays(2).toLocalDate(),
                        ),
                ),
            ).oppgaver.size shouldBe 1

            repo.søk(
                Søkefilter(
                    tilstander = setOf(Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING),
                    periode =
                        Periode(
                            fom = opprettetNå.toLocalDate(),
                            tom = opprettetNå.toLocalDate(),
                        ),
                ),
            ).oppgaver.size shouldBe 1

            repo.søk(
                Søkefilter(
                    tilstander = emptySet(),
                    periode =
                        Periode(
                            fom = opprettetNå.toLocalDate(),
                            tom = opprettetNå.toLocalDate(),
                        ),
                ),
            ).oppgaver.size shouldBe 1
        }
    }

    @Test
    fun `Skal kunne søke etter oppgaver opprettet en bestemt dato, uavhengig av tid på døgnet`() {
        DBTestHelper.Companion.withMigratedDb { ds ->
            val iDag = LocalDate.now()
            val iGår: LocalDate = iDag.minusDays(1)
            val iForgårs = iDag.minusDays(2)
            val iForgårsSåSeintPåDagenSomMulig = LocalDateTime.of(iForgårs, LocalTime.MAX)
            val iGårSåTidligPåDagenSomMulig = LocalDateTime.of(iGår, LocalTime.MIN)
            val iGårSåSeintPåDagenSomMulig = LocalDateTime.of(iGår, LocalTime.MAX)
            val iDagSåTidligPåDagenSomMulig = LocalDateTime.of(iDag, LocalTime.MIN)
            val oppgaveOpprettetTidligIGår =
                this.leggTilOppgave(tilstand = Oppgave.KlarTilBehandling, opprettet = iGårSåTidligPåDagenSomMulig)
            val oppgaveOpprettetSeintIGår =
                this.leggTilOppgave(tilstand = Oppgave.KlarTilBehandling, opprettet = iGårSåSeintPåDagenSomMulig)
            this.leggTilOppgave(tilstand = Oppgave.KlarTilBehandling, opprettet = iForgårsSåSeintPåDagenSomMulig)
            this.leggTilOppgave(tilstand = Oppgave.KlarTilBehandling, opprettet = iDagSåTidligPåDagenSomMulig)

            val repo = PostgresOppgaveRepository(ds)
            val oppgaver =
                repo.søk(
                    Søkefilter(
                        tilstander = setOf(Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING),
                        periode = Periode(fom = iGår, tom = iGår),
                    ),
                )
            oppgaver.oppgaver.size shouldBe 2
            oppgaver.oppgaver.contains(oppgaveOpprettetTidligIGår)
            oppgaver.oppgaver.contains(oppgaveOpprettetSeintIGår)
        }
    }

    @Test
    fun `Skal hente en oppgave basert på behandlingId`() {
        val oppgave = lagOppgave()
        DBTestHelper.Companion.withOppgave(oppgave = oppgave) { ds ->
            val repo = PostgresOppgaveRepository(ds)
            repo.hentOppgaveFor(oppgave.behandlingId) shouldBe oppgave

            shouldThrow<DataNotFoundException> {
                repo.hentOppgaveFor(behandlingId = UUIDv7.ny())
            }
        }
    }

    @Test
    fun `Skal finne en oppgave basert på behandlingId hvis den finnes`() {
        val oppgave = lagOppgave()
        DBTestHelper.Companion.withOppgave(oppgave) { ds ->
            val repo = PostgresOppgaveRepository(ds)
            repo.finnOppgaveFor(oppgave.behandlingId) shouldBe oppgave
            repo.finnOppgaveFor(behandlingId = UUIDv7.ny()) shouldBe null
        }
    }

    @Test
    fun `Hent adressegraderingsbeskyttelse for person gitt oppgave`() {
        val oppgave =
            lagOppgave(
                person =
                    lagPerson(
                        ident = testPerson.ident,
                        addresseBeskyttelseGradering = STRENGT_FORTROLIG,
                    ),
            )
        DBTestHelper.Companion.withOppgave(oppgave = oppgave) { ds ->
            PostgresOppgaveRepository(ds)
                .adresseGraderingForPerson(oppgave.oppgaveId) shouldBe STRENGT_FORTROLIG
        }
    }

    @Test
    fun `Skal kunne lagre og hente en oppgave med SkriptHendelse i logginnslaget`() {
        val behandling = lagBehandling()
        val tilstandsendring =
            Tilstandsendring(
                tilstand = Oppgave.Tilstand.Type.AVBRUTT,
                hendelse = SkriptHendelse(Applikasjon("Dette er et skript")),
                tidspunkt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
            )
        val testOppgave =
            lagOppgave(
                behandlingId = behandling.behandlingId,
                tilstandslogg = OppgaveTilstandslogg(tilstandsendring),
            )
        DBTestHelper.Companion.withBehandling(behandling = behandling) { ds ->
            val repo = PostgresOppgaveRepository(ds)
            repo.lagre(testOppgave)
            val oppgaveFraDatabase = repo.hentOppgave(testOppgave.oppgaveId)
            oppgaveFraDatabase.tilstandslogg shouldBe testOppgave.tilstandslogg
        }
    }
}
