package no.nav.dagpenger.saksbehandling.db

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.FORTROLIG
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.STRENGT_FORTROLIG
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.Applikasjon
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.BehandlingType
import no.nav.dagpenger.saksbehandling.BehandlingType.KLAGE
import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg.AVSLAG_MINSTEINNTEKT
import no.nav.dagpenger.saksbehandling.Emneknagg.Regelknagg.INNVILGELSE
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Oppgave.FerdigBehandlet
import no.nav.dagpenger.saksbehandling.Oppgave.KlarTilBehandling
import no.nav.dagpenger.saksbehandling.Oppgave.Opprettet
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.BEHANDLES_I_ARENA
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.Companion.søkbareTilstander
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_KONTROLL
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.PAA_VENT
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_KONTROLL
import no.nav.dagpenger.saksbehandling.Oppgave.UnderBehandling
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.TilgangType.BESLUTTER
import no.nav.dagpenger.saksbehandling.TilgangType.EGNE_ANSATTE
import no.nav.dagpenger.saksbehandling.TilgangType.FORTROLIG_ADRESSE
import no.nav.dagpenger.saksbehandling.TilgangType.SAKSBEHANDLER
import no.nav.dagpenger.saksbehandling.TilgangType.STRENGT_FORTROLIG_ADRESSE
import no.nav.dagpenger.saksbehandling.TilgangType.STRENGT_FORTROLIG_ADRESSE_UTLAND
import no.nav.dagpenger.saksbehandling.Tilstandsendring
import no.nav.dagpenger.saksbehandling.Tilstandslogg
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.adressebeskyttelseTilganger
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.db.oppgave.DataNotFoundException
import no.nav.dagpenger.saksbehandling.db.oppgave.Periode
import no.nav.dagpenger.saksbehandling.db.oppgave.Periode.Companion.UBEGRENSET_PERIODE
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.db.oppgave.Søkefilter
import no.nav.dagpenger.saksbehandling.db.oppgave.Søkefilter.Paginering
import no.nav.dagpenger.saksbehandling.db.oppgave.TildelNesteOppgaveFilter
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
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class PostgresOppgaveRepositoryTest {
    private val saksbehandler =
        Saksbehandler("saksbehandler", setOf("SaksbehandlerADGruppe"), setOf(SAKSBEHANDLER))
    private val beslutter =
        Saksbehandler(
            "beslutter",
            setOf("SaksbehandlerADGruppe", "BeslutterADGruppe"),
            setOf(BESLUTTER, SAKSBEHANDLER),
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
                type = BehandlingType.RETT_TIL_DAGPENGER,
                opprettet = LocalDateTime.now(),
                hendelse = TomHendelse,
            )

        DBTestHelper.withBehandling(behandling = behandling) { ds ->
            val repo = PostgresOppgaveRepository(ds)
            val saksbehandler =
                Saksbehandler(
                    navIdent = "NAVIdent2",
                    grupper = emptySet(),
                )
            val oppgave = lagOppgave(tilstand = FerdigBehandlet, behandlingId = behandling.behandlingId)
            repo.lagre(oppgave)
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
                        adressebeskyttelseTilganger = setOf(FORTROLIG),
                        navIdent = saksbehandler.navIdent,
                    ),
            ) shouldBe null
        }
    }

    @Test
    fun `Tildel neste ledige klage-oppgave`() {
        val klageBehandling = lagBehandling(type = KLAGE)
        val søknadBehandling = lagBehandling(type = BehandlingType.RETT_TIL_DAGPENGER)
        DBTestHelper.withBehandlinger(
            person = testPerson,
            behandlinger = listOf(klageBehandling, søknadBehandling),
        ) { ds ->
            val repo = PostgresOppgaveRepository(ds)

            lagOppgave(
                tilstand = KlarTilBehandling,
                opprettet = opprettetNå.minusDays(1),
                behandlingId = søknadBehandling.behandlingId,
                person = testPerson,
            ).also { repo.lagre(it) }

            val klageOppgave =
                lagOppgave(
                    tilstand = KlarTilBehandling,
                    opprettet = opprettetNå,
                    behandlingId = klageBehandling.behandlingId,
                    person = testPerson,
                    behandlingType = KLAGE,
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
                            periode = UBEGRENSET_PERIODE,
                            emneknagg = emptySet(),
                            behandlingTyper = setOf(KLAGE),
                            egneAnsatteTilgang = false,
                            adressebeskyttelseTilganger = setOf(UGRADERT),
                            navIdent = saksbehandler.navIdent,
                        ),
                )!!

            nesteOppgave.oppgaveId shouldBe klageOppgave.oppgaveId
            nesteOppgave.behandlerIdent shouldBe saksbehandler.navIdent
            nesteOppgave.tilstand().type shouldBe UNDER_BEHANDLING
        }
    }

    @Test
    fun `Finn neste ledige oppgave som ikke gjelder egne ansatte`() {
        DBTestHelper.withMigratedDb { ds ->
            val eldsteOppgaveMedSkjermingSomEgneAnsatte =
                this.leggTilOppgave(
                    tilstand = KlarTilBehandling,
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
                    tilstand = KlarTilBehandling,
                    opprettet = opprettetNå.minusDays(1),
                    person =
                        Person(
                            ident = "11111222222",
                            skjermesSomEgneAnsatte = false,
                            adressebeskyttelseGradering = UGRADERT,
                        ),
                )

            val nyesteOppgaveUtenSkjermingAvEgenAnsatt =
                this.leggTilOppgave(
                    tilstand = KlarTilBehandling,
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
                            periode = UBEGRENSET_PERIODE,
                            emneknagg = emptySet(),
                            egneAnsatteTilgang = false,
                            adressebeskyttelseTilganger = setOf(UGRADERT),
                            navIdent = saksbehandlerUtenTilgangTilEgneAnsatte.navIdent,
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
                            egneAnsatteTilgang = true,
                            adressebeskyttelseTilganger = setOf(UGRADERT),
                            navIdent = saksbehandlerMedTilgangTilEgneAnsatte.navIdent,
                        ),
                )!!

            nesteOppgaveMedTilgang.oppgaveId shouldBe eldsteOppgaveMedSkjermingSomEgneAnsatte.oppgaveId
            nesteOppgaveMedTilgang.behandlerIdent shouldBe saksbehandlerMedTilgangTilEgneAnsatte.navIdent
            nesteOppgaveMedTilgang.tilstand().type shouldBe UNDER_BEHANDLING
        }
    }

    @Test
    fun `Finn neste ledige oppgave som ikke gjelder adressebeskyttede personer`() {
        DBTestHelper.withMigratedDb { ds ->

            val eldsteOppgaveMedAdressebeskyttelse =
                this.leggTilOppgave(
                    tilstand = KlarTilBehandling,
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
                    tilstand = KlarTilBehandling,
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
                            periode = UBEGRENSET_PERIODE,
                            emneknagg = emptySet(),
                            egneAnsatteTilgang = false,
                            adressebeskyttelseTilganger = setOf(UGRADERT),
                            harBeslutterRolle = false,
                            navIdent = saksbehandlernUtenTilgangTilAdressebeskyttede.navIdent,
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
                            egneAnsatteTilgang = true,
                            adressebeskyttelseTilganger = setOf(UGRADERT, FORTROLIG),
                            navIdent = saksbehandlerMedTilgangTilEgneAnsatte.navIdent,
                        ),
                )!!

            nesteOppgaveMedTilgang.oppgaveId shouldBe eldsteOppgaveMedAdressebeskyttelse.oppgaveId
            nesteOppgaveMedTilgang.behandlerIdent shouldBe saksbehandlerMedTilgangTilEgneAnsatte.navIdent
            nesteOppgaveMedTilgang.tilstand().type shouldBe UNDER_BEHANDLING
        }
    }

    @Test
    fun `Ved tildeling av neste oppgave, skal det lagres en tilstandsendring i tilstandsendringsloggen`() {
        DBTestHelper.withMigratedDb { ds ->
            val oppgave =
                this.leggTilOppgave(
                    tilstand = KlarTilBehandling,
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
                    periode = UBEGRENSET_PERIODE,
                    emneknagg = setOf("Testknagg"),
                    adressebeskyttelseTilganger = setOf(UGRADERT),
                    navIdent = saksbehandler.navIdent,
                )
            val nesteOppgave = repo.tildelOgHentNesteOppgave(nesteOppgaveHendelse, filter)
            nesteOppgave!!.tilstandslogg.size shouldBe antallTilstandsendringer + 1
        }
    }

    @Test
    fun `Skal ikke få tildelt kontrolloppgave som man selv har saksbehandlet`() {
        DBTestHelper.withMigratedDb { ds ->
            val saksbehandlerUtført =
                Saksbehandler(
                    navIdent = "saksbehandlerUtført",
                    grupper = emptySet(),
                    tilganger = setOf(SAKSBEHANDLER, BESLUTTER),
                )

            val annenBeslutter =
                Saksbehandler(
                    navIdent = "annenBeslutter",
                    grupper = emptySet(),
                    tilganger = setOf(SAKSBEHANDLER, BESLUTTER),
                )
            val oppgaveId = UUIDv7.ny()

            val tilstandsloggUnderBehandling =
                Tilstandslogg(
                    tilstandsendringer =
                        mutableListOf(
                            Tilstandsendring(
                                tilstand = UNDER_BEHANDLING,
                                hendelse =
                                    NesteOppgaveHendelse(
                                        ansvarligIdent = annenBeslutter.navIdent,
                                        utførtAv = annenBeslutter,
                                    ),
                                tidspunkt = LocalDateTime.now().minusDays(3),
                            ),
                            Tilstandsendring(
                                tilstand = UNDER_BEHANDLING,
                                hendelse =
                                    NesteOppgaveHendelse(
                                        ansvarligIdent = saksbehandlerUtført.navIdent,
                                        utførtAv = saksbehandlerUtført,
                                    ),
                                tidspunkt = LocalDateTime.now().minusDays(2),
                            ),
                            Tilstandsendring(
                                tilstand = PAA_VENT,
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
                                tilstand = UNDER_BEHANDLING,
                                hendelse =
                                    PåVentFristUtgåttHendelse(
                                        oppgaveId = oppgaveId,
                                        utførtAv = Applikasjon("dp-saksbehandling"),
                                    ),
                                tidspunkt = LocalDateTime.now().minusHours(1),
                            ),
                            Tilstandsendring(
                                tilstand = KLAR_TIL_KONTROLL,
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
                        periode = UBEGRENSET_PERIODE,
                        emneknagg = emptySet(),
                        egneAnsatteTilgang = saksbehandlerUtført.tilganger.contains(EGNE_ANSATTE),
                        adressebeskyttelseTilganger = saksbehandlerUtført.adressebeskyttelseTilganger(),
                        harBeslutterRolle = saksbehandlerUtført.tilganger.contains(BESLUTTER),
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
                        periode = UBEGRENSET_PERIODE,
                        emneknagg = emptySet(),
                        egneAnsatteTilgang = annenBeslutter.tilganger.contains(EGNE_ANSATTE),
                        adressebeskyttelseTilganger = annenBeslutter.adressebeskyttelseTilganger(),
                        harBeslutterRolle = annenBeslutter.tilganger.contains(BESLUTTER),
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
        DBTestHelper.withMigratedDb { ds ->
            val saksbehandlerUtført =
                Saksbehandler(
                    navIdent = "saksbehandlerUtført",
                    grupper = emptySet(),
                    tilganger = setOf(SAKSBEHANDLER),
                )

            fun tilstandsloggUnderBehandling() =
                Tilstandslogg(
                    tilstandsendringer =
                        mutableListOf(
                            Tilstandsendring(
                                tilstand = UNDER_BEHANDLING,
                                hendelse =
                                    NesteOppgaveHendelse(
                                        ansvarligIdent = saksbehandlerUtført.navIdent,
                                        utførtAv = saksbehandlerUtført,
                                    ),
                            ),
                        ),
                )

            val testSaksbehandler =
                Saksbehandler(
                    navIdent = "saksbehandler",
                    grupper = emptySet(),
                    tilganger = setOf(SAKSBEHANDLER),
                )
            val vanligBeslutter =
                Saksbehandler(
                    navIdent = "vanligBeslutter",
                    grupper = emptySet(),
                    tilganger = setOf(SAKSBEHANDLER, BESLUTTER),
                )
            val beslutterEgneAnsatte =
                Saksbehandler(
                    navIdent = "beslutterEgneAnsatte",
                    grupper = emptySet(),
                    tilganger = setOf(SAKSBEHANDLER, BESLUTTER, EGNE_ANSATTE),
                )
            val beslutterFortroligAdresse =
                Saksbehandler(
                    navIdent = "beslutterFortroligAdresse",
                    grupper = emptySet(),
                    tilganger = setOf(SAKSBEHANDLER, BESLUTTER, FORTROLIG_ADRESSE),
                )
            val beslutterStrengtFortroligAdresse =
                Saksbehandler(
                    navIdent = "beslutterStrengtFortroligAdresse",
                    grupper = emptySet(),
                    tilganger = setOf(SAKSBEHANDLER, BESLUTTER, STRENGT_FORTROLIG_ADRESSE),
                )
            val beslutterStrengtFortroligAdresseUtland =
                Saksbehandler(
                    navIdent = "beslutterStrengtFortroligAdresseUtland",
                    grupper = emptySet(),
                    tilganger = setOf(SAKSBEHANDLER, BESLUTTER, STRENGT_FORTROLIG_ADRESSE_UTLAND),
                )
            val beslutterStrengtFortroligOgEgneAnsatte =
                Saksbehandler(
                    navIdent = "beslutterStrengtFortroligOgEgneAnsatte",
                    grupper = emptySet(),
                    tilganger = setOf(SAKSBEHANDLER, BESLUTTER, STRENGT_FORTROLIG_ADRESSE, EGNE_ANSATTE),
                )

            val yngsteLedigeOppgaveOpprettetIDag =
                this.leggTilOppgave(
                    tilstand = KlarTilBehandling,
                    opprettet = opprettetNå,
                )

            val oppgaveMedEmneknagg =
                this.leggTilOppgave(
                    tilstand = KlarTilBehandling,
                    opprettet = opprettetNå.minusDays(5),
                    emneknagger = setOf("Testknagg"),
                )

            val eldsteLedigeOppgaveKlarTilBehandling =
                this.leggTilOppgave(
                    tilstand = KlarTilBehandling,
                    opprettet = opprettetNå.minusDays(10),
                )

            val endaEldreTildeltOppgave =
                this.leggTilOppgave(
                    tilstand = KlarTilBehandling,
                    opprettet = opprettetNå.minusDays(11),
                    saksBehandlerIdent = saksbehandler.navIdent,
                )

            val endaEldreFerdigOppgave =
                this.leggTilOppgave(
                    tilstand = FerdigBehandlet,
                    opprettet = opprettetNå.minusDays(12),
                    saksBehandlerIdent = testSaksbehandler.navIdent,
                    tilstandslogg = tilstandsloggUnderBehandling(),
                )

            val endaEldreOpprettetOppgave =
                this.leggTilOppgave(
                    tilstand = Opprettet,
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
                    person = lagPerson(addresseBeskyttelseGradering = STRENGT_FORTROLIG_UTLAND),
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
                    periode = UBEGRENSET_PERIODE,
                    emneknagg = setOf("Testknagg"),
                    adressebeskyttelseTilganger = setOf(UGRADERT),
                    navIdent = testSaksbehandler.navIdent,
                )
            val opprettetIDagFilterForTestSaksbehandler =
                TildelNesteOppgaveFilter(
                    periode = Periode(fom = opprettetNå.toLocalDate(), tom = opprettetNå.toLocalDate()),
                    emneknagg = emptySet(),
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
                    it.tilstand() shouldBe UnderBehandling
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
                    it.tilstand() shouldBe UnderBehandling
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
                        periode = UBEGRENSET_PERIODE,
                        emneknagg = emptySet(),
                        egneAnsatteTilgang = testSaksbehandler.tilganger.contains(EGNE_ANSATTE),
                        adressebeskyttelseTilganger = testSaksbehandler.adressebeskyttelseTilganger(),
                        harBeslutterRolle = testSaksbehandler.tilganger.contains(BESLUTTER),
                        navIdent = testSaksbehandler.navIdent,
                    ),
            ).let {
                assertSoftly {
                    require(it != null) { "Skal finne en oppgave" }
                    it.oppgaveId shouldBe eldsteLedigeOppgaveKlarTilBehandling.oppgaveId
                    it.behandlerIdent shouldBe testSaksbehandler.navIdent
                    it.tilstand() shouldBe UnderBehandling
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
                        periode = UBEGRENSET_PERIODE,
                        emneknagg = emptySet(),
                        egneAnsatteTilgang = beslutter.tilganger.contains(EGNE_ANSATTE),
                        adressebeskyttelseTilganger = beslutter.adressebeskyttelseTilganger(),
                        harBeslutterRolle = beslutter.tilganger.contains(BESLUTTER),
                        navIdent = beslutter.navIdent,
                    ),
            ).let {
                assertSoftly {
                    require(it != null) { "Skal finne en oppgave" }
                    it.oppgaveId shouldBe eldsteKontrollOppgaveUtenSkjermingOgAdressegradering.oppgaveId
                    it.tilstand().type shouldBe UNDER_KONTROLL
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
                        periode = UBEGRENSET_PERIODE,
                        emneknagg = emptySet(),
                        egneAnsatteTilgang = beslutterEgneAnsatte.tilganger.contains(EGNE_ANSATTE),
                        adressebeskyttelseTilganger = beslutterEgneAnsatte.adressebeskyttelseTilganger(),
                        harBeslutterRolle = beslutterEgneAnsatte.tilganger.contains(BESLUTTER),
                        navIdent = beslutterEgneAnsatte.navIdent,
                    ),
            ).let {
                assertSoftly {
                    require(it != null) { "Skal finne en oppgave" }
                    it.oppgaveId shouldBe eldsteKontrollOppgaveEgneAnsatteSkjerming.oppgaveId
                    it.tilstand().type shouldBe UNDER_KONTROLL
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
                        periode = UBEGRENSET_PERIODE,
                        emneknagg = emptySet(),
                        egneAnsatteTilgang = beslutterFortroligAdresse.tilganger.contains(EGNE_ANSATTE),
                        adressebeskyttelseTilganger = beslutterFortroligAdresse.adressebeskyttelseTilganger(),
                        harBeslutterRolle = beslutterFortroligAdresse.tilganger.contains(BESLUTTER),
                        navIdent = beslutterFortroligAdresse.navIdent,
                    ),
            ).let {
                assertSoftly {
                    require(it != null) { "Skal finne en oppgave" }
                    it.oppgaveId shouldBe eldsteKontrollOppgaveFortroligAdresse.oppgaveId
                    it.tilstand().type shouldBe UNDER_KONTROLL
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
                        periode = UBEGRENSET_PERIODE,
                        emneknagg = emptySet(),
                        egneAnsatteTilgang = beslutterStrengtFortroligAdresse.tilganger.contains(EGNE_ANSATTE),
                        adressebeskyttelseTilganger = beslutterStrengtFortroligAdresse.adressebeskyttelseTilganger(),
                        harBeslutterRolle = beslutterStrengtFortroligAdresse.tilganger.contains(BESLUTTER),
                        navIdent = beslutterStrengtFortroligAdresse.navIdent,
                    ),
            ).let {
                assertSoftly {
                    require(it != null) { "Skal finne en oppgave" }
                    it.oppgaveId shouldBe eldsteKontrollOppgaveStrengtFortroligAdresse.oppgaveId
                    it.tilstand().type shouldBe UNDER_KONTROLL
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
                        periode = UBEGRENSET_PERIODE,
                        emneknagg = emptySet(),
                        egneAnsatteTilgang = beslutterStrengtFortroligAdresseUtland.tilganger.contains(EGNE_ANSATTE),
                        adressebeskyttelseTilganger = beslutterStrengtFortroligAdresseUtland.adressebeskyttelseTilganger(),
                        harBeslutterRolle = beslutterStrengtFortroligAdresseUtland.tilganger.contains(BESLUTTER),
                        navIdent = beslutterStrengtFortroligAdresseUtland.navIdent,
                    ),
            ).let {
                assertSoftly {
                    require(it != null) { "Skal finne en oppgave" }
                    it.oppgaveId shouldBe eldsteKontrollOppgaveStrengtFortroligAdresseUtland.oppgaveId
                    it.tilstand().type shouldBe UNDER_KONTROLL
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
                        periode = UBEGRENSET_PERIODE,
                        emneknagg = emptySet(),
                        egneAnsatteTilgang =
                            beslutterStrengtFortroligOgEgneAnsatte.tilganger.contains(
                                EGNE_ANSATTE,
                            ),
                        adressebeskyttelseTilganger = beslutterStrengtFortroligOgEgneAnsatte.adressebeskyttelseTilganger(),
                        harBeslutterRolle =
                            beslutterStrengtFortroligOgEgneAnsatte.tilganger.contains(
                                BESLUTTER,
                            ),
                        navIdent = beslutterStrengtFortroligOgEgneAnsatte.navIdent,
                    ),
            ).let {
                assertSoftly {
                    require(it != null) { "Skal finne en oppgave" }
                    it.oppgaveId shouldBe eldsteKontrollOppgaveStrengtFortroligAdresseOgEgneAnsatteSkjerming.oppgaveId
                    it.tilstand().type shouldBe UNDER_KONTROLL
                    it.sisteBeslutter() shouldBe beslutterStrengtFortroligOgEgneAnsatte.navIdent
                }
            }
        }
    }

    @Test
    fun `Skal kunne lagre både en behandling og en oppgave flere ganger`() {
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
    fun `Skal kunne lagre og hente en oppgave med notat`() {
        val testOppgave = lagOppgave(tilstand = Oppgave.KlarTilKontroll)
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

        withMigratedDb { ds ->
            val repo = PostgresOppgaveRepository(ds)
            repo.lagre(testOppgave)
            val oppgaveFraDatabase = repo.hentOppgave(testOppgave.oppgaveId)
            oppgaveFraDatabase shouldBe testOppgave
        }
    }

    @Test
    fun `Skal kunne lagre notatet til en oppgave`() {
        val oppgave = lagOppgave(tilstand = Oppgave.KlarTilKontroll)
        withMigratedDb { ds ->
            val repo = PostgresOppgaveRepository(ds)
            repo.lagre(oppgave)
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
        withMigratedDb { ds ->
            val repo = PostgresOppgaveRepository(ds)
            repo.lagre(oppgave)
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
        val testOppgave = lagOppgave()
        withMigratedDb { ds ->
            val repo = PostgresOppgaveRepository(ds)
            repo.lagre(testOppgave)
            val oppgaveFraDatabase = repo.hentOppgave(testOppgave.oppgaveId)
            oppgaveFraDatabase shouldBe testOppgave
        }
    }

    @Test
    fun `Skal kunne slette et notat for en oppgave`() {
        val testOppgave = lagOppgave(tilstand = Oppgave.KlarTilKontroll)
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

        withMigratedDb { ds ->
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
            Tilstandslogg(
                mutableListOf(
                    Tilstandsendring(
                        tilstand = KLAR_TIL_KONTROLL,
                        hendelse =
                            SendTilKontrollHendelse(
                                oppgaveId = oppgaveIdTest,
                                utførtAv = saksbehandler,
                            ),
                        tidspunkt = nå.minusDays(2).truncatedTo(ChronoUnit.SECONDS),
                    ),
                    Tilstandsendring(
                        tilstand = UNDER_KONTROLL,
                        hendelse =
                            SettOppgaveAnsvarHendelse(
                                oppgaveId = oppgaveIdTest,
                                beslutter.navIdent,
                                beslutter,
                            ),
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
    fun `CRUD på oppgave i tilstand PAA_VENT`() {
        val testOppgave = lagOppgave(tilstand = UnderBehandling)
        val utsattTil = LocalDate.now().plusDays(1)
        withMigratedDb { ds ->
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
    fun `Skal kunne søke etter oppgaver filtrert på behandlingstype`() {
        val søknadOppgave =
            lagOppgave(tilstand = KlarTilBehandling, behandlingType = BehandlingType.RETT_TIL_DAGPENGER)
        val klageOppgave = lagOppgave(tilstand = KlarTilBehandling, behandlingType = KLAGE)
        withMigratedDb { ds ->
            val repo = PostgresOppgaveRepository(ds)
            repo.lagre(søknadOppgave)
            repo.lagre(klageOppgave)
            repo.søk(
                søkeFilter =
                    Søkefilter(
                        tilstander = Oppgave.Tilstand.Type.entries.toSet(),
                        periode = UBEGRENSET_PERIODE,
                        emneknagger = emptySet(),
                        behandlingTyper = setOf(KLAGE),
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
        val kari =
            Person(
                ident = "10987654321",
                skjermesSomEgneAnsatte = false,
                adressebeskyttelseGradering = UGRADERT,
            )

        val oppgave1TilOla = lagOppgave(person = ola, tilstand = KlarTilBehandling, opprettet = opprettetNå)
        val oppgave2TilOla =
            lagOppgave(person = ola, tilstand = FerdigBehandlet, opprettet = opprettetNå.minusDays(1))
        val oppgave1TilKari = lagOppgave(person = kari, tilstand = FerdigBehandlet)

        withMigratedDb { ds ->
            val repo = PostgresOppgaveRepository(ds)
            repo.lagre(oppgave1TilOla)
            repo.lagre(oppgave2TilOla)
            repo.lagre(oppgave1TilKari)

            repo.finnOppgaverFor(ola.ident) shouldBe listOf(oppgave2TilOla, oppgave1TilOla)
            repo.finnOppgaverFor(kari.ident) shouldBe listOf(oppgave1TilKari)
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
        val oppgaveKlarTilBehandling =
            lagOppgave(
                tilstand = KlarTilBehandling,
                person = person,
                behandlingId = hendelse.behandlingId,
            )

        withMigratedDb { ds ->
            val repo = PostgresOppgaveRepository(ds)
            repo.lagre(oppgaveKlarTilBehandling)

            repo.oppgaveTilstandForSøknad(
                ident = hendelse.ident,
                søknadId = hendelse.søknadId,
            ) shouldBe KLAR_TIL_BEHANDLING

            repo.oppgaveTilstandForSøknad(
                ident = hendelse.ident,
                søknadId = UUIDv7.ny(),
            ) shouldBe null
        }
    }

    @Test
    fun `Skal hente oppgaveId fra behandlingId`() {
        val behandlingId = UUIDv7.ny()
        val oppgave = lagOppgave(behandlingId = behandlingId, behandlingType = BehandlingType.RETT_TIL_DAGPENGER)

        withMigratedDb { ds ->
            val repo = PostgresOppgaveRepository(ds)
            repo.lagre(oppgave)

            repo.hentOppgaveIdFor(behandlingId = behandlingId) shouldBe oppgave.oppgaveId
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
                    tilstander = Oppgave.Tilstand.Type.entries.toSet(),
                    periode = UBEGRENSET_PERIODE,
                    emneknagger = emptySet(),
                ),
            ).oppgaver shouldBe listOf(oppgave1, oppgave2, oppgave3)

            repo.søk(
                Søkefilter(
                    tilstander = Oppgave.Tilstand.Type.entries.toSet(),
                    periode = UBEGRENSET_PERIODE,
                    emneknagger = setOf("hubba"),
                ),
            ).oppgaver shouldBe listOf(oppgave1, oppgave2)

            repo.søk(
                Søkefilter(
                    tilstander = Oppgave.Tilstand.Type.entries.toSet(),
                    periode = UBEGRENSET_PERIODE,
                    emneknagger = setOf("bubba"),
                ),
            ).oppgaver shouldBe listOf(oppgave1)

            repo.søk(
                Søkefilter(
                    tilstander = Oppgave.Tilstand.Type.entries.toSet(),
                    periode = UBEGRENSET_PERIODE,
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

        withMigratedDb { ds ->
            val repo = PostgresOppgaveRepository(ds)
            val oppgave1 =
                lagOppgave(
                    UnderBehandling,
                    enUkeSiden,
                    saksbehandler1,
                    emneknagger = setOf(INNVILGELSE.visningsnavn),
                )
            val oppgave2 =
                lagOppgave(
                    UnderBehandling,
                    saksbehandlerIdent = saksbehandler2,
                    emneknagger = setOf(AVSLAG_MINSTEINNTEKT.visningsnavn),
                )
            val oppgave3 =
                lagOppgave(
                    FerdigBehandlet,
                    saksbehandlerIdent = saksbehandler2,
                    emneknagger = setOf(INNVILGELSE.visningsnavn),
                )
            val oppgave4 =
                lagOppgave(
                    UnderBehandling,
                    saksbehandlerIdent = null,
                    emneknagger = setOf(INNVILGELSE.visningsnavn),
                )

            repo.lagre(oppgave1)
            repo.lagre(oppgave2)
            repo.lagre(oppgave3)
            repo.lagre(oppgave4)

            repo.søk(
                Søkefilter(
                    tilstander = Oppgave.Tilstand.Type.entries.toSet(),
                    periode = UBEGRENSET_PERIODE,
                    saksbehandlerIdent = saksbehandler1,
                ),
            ).oppgaver.size shouldBe 1

            repo.søk(
                Søkefilter(
                    tilstander = Oppgave.Tilstand.Type.entries.toSet(),
                    periode = UBEGRENSET_PERIODE,
                    saksbehandlerIdent = saksbehandler2,
                ),
            ).oppgaver.size shouldBe 2

            repo.søk(
                Søkefilter(
                    tilstander = Oppgave.Tilstand.Type.entries.toSet(),
                    periode = UBEGRENSET_PERIODE,
                    saksbehandlerIdent = null,
                ),
            ).oppgaver.size shouldBe 4

            repo.søk(
                Søkefilter(
                    tilstander = Oppgave.Tilstand.Type.entries.toSet(),
                    periode = UBEGRENSET_PERIODE,
                    saksbehandlerIdent = saksbehandler2,
                    emneknagger = setOf(INNVILGELSE.visningsnavn),
                ),
            ).oppgaver.size shouldBe 1
        }
    }

    @Test
    fun `Skal kunne hente paginerte oppgaver`() {
        withMigratedDb { ds ->
            val repo = PostgresOppgaveRepository(ds)
            val nyesteOppgave = lagOppgave(opprettet = opprettetNå)
            val nestNyesteOppgave = lagOppgave(opprettet = opprettetNå.minusDays(1))
            val nestEldsteOppgave = lagOppgave(opprettet = opprettetNå.minusDays(3))
            val eldsteOppgave = lagOppgave(opprettet = opprettetNå.minusDays(7))

            repo.lagre(nestEldsteOppgave)
            repo.lagre(nyesteOppgave)
            repo.lagre(nestNyesteOppgave)
            repo.lagre(eldsteOppgave)

            repo.søk(
                Søkefilter(
                    tilstander = søkbareTilstander,
                    periode = UBEGRENSET_PERIODE,
                    paginering = null,
                ),
            ).let {
                it.oppgaver.size shouldBe 4
                it.totaltAntallOppgaver shouldBe 4
            }

            repo.søk(
                Søkefilter(
                    tilstander = søkbareTilstander,
                    periode = UBEGRENSET_PERIODE,
                    paginering = Paginering(2, 0),
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
                    periode = UBEGRENSET_PERIODE,
                    paginering = Paginering(2, 1),
                ),
            ).let {
                it.oppgaver.size shouldBe 2
                it.oppgaver[0] shouldBe nestNyesteOppgave
                it.oppgaver[1] shouldBe nyesteOppgave
                it.totaltAntallOppgaver shouldBe 4
            }

            repo.søk(
                Søkefilter(
                    tilstander = søkbareTilstander,
                    periode = UBEGRENSET_PERIODE,
                    paginering = Paginering(10, 0),
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
                    tilstander = søkbareTilstander,
                    periode = UBEGRENSET_PERIODE,
                    paginering = Paginering(10, 1),
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
                    tilstander = setOf(UNDER_BEHANDLING),
                    periode = UBEGRENSET_PERIODE,
                ),
            ).oppgaver.single() shouldBe oppgaveUnderBehandlingEnUkeGammel

            repo.søk(
                Søkefilter(
                    tilstander = setOf(KLAR_TIL_BEHANDLING, UNDER_BEHANDLING),
                    periode = UBEGRENSET_PERIODE,
                ),
            ).oppgaver.size shouldBe 3

            repo.søk(
                Søkefilter(
                    periode = UBEGRENSET_PERIODE,
                    tilstander = søkbareTilstander,
                    saksbehandlerIdent = null,
                    personIdent = null,
                    oppgaveId = null,
                    behandlingId = null,
                ),
            ).let {
                it.oppgaver.size shouldBe 3
                it.oppgaver.map { oppgave -> oppgave.tilstand().type }.toSet() shouldBe
                    setOf(
                        UNDER_BEHANDLING,
                        KLAR_TIL_BEHANDLING,
                    )
            }

            repo.søk(
                Søkefilter(
                    tilstander = setOf(KLAR_TIL_BEHANDLING),
                    periode =
                        Periode(
                            fom = enUkeSiden.plusDays(1).toLocalDate(),
                            tom = enUkeSiden.plusDays(2).toLocalDate(),
                        ),
                ),
            ).oppgaver.size shouldBe 0

            repo.søk(
                Søkefilter(
                    tilstander = setOf(UNDER_BEHANDLING),
                    periode =
                        Periode(
                            fom = enUkeSiden.minusDays(1).toLocalDate(),
                            tom = enUkeSiden.plusDays(2).toLocalDate(),
                        ),
                ),
            ).oppgaver.size shouldBe 1

            repo.søk(
                Søkefilter(
                    tilstander = setOf(KLAR_TIL_BEHANDLING),
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
                        tilstander = setOf(KLAR_TIL_BEHANDLING),
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

    @Test
    fun `Skal kunne lagre og hente en oppgave med SkriptHendelse i logginnslaget`() {
        val tilstandsendring =
            Tilstandsendring(
                tilstand = BEHANDLES_I_ARENA,
                hendelse = SkriptHendelse(Applikasjon("Dette er et skript")),
                tidspunkt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS),
            )
        val testOppgave = lagOppgave(tilstandslogg = Tilstandslogg(mutableListOf(tilstandsendring)))
        withMigratedDb { ds ->
            val repo = PostgresOppgaveRepository(ds)
            repo.lagre(testOppgave)
            val oppgaveFraDatabase = repo.hentOppgave(testOppgave.oppgaveId)
            oppgaveFraDatabase.tilstandslogg shouldBe testOppgave.tilstandslogg
        }
    }
}
