package no.nav.dagpenger.saksbehandling.statistikk.db

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.Configuration
import no.nav.dagpenger.saksbehandling.Emneknagg
import no.nav.dagpenger.saksbehandling.Emneknagg.AvbrytBehandling.AVBRUTT_FLERE_SØKNADER
import no.nav.dagpenger.saksbehandling.HendelseBehandler
import no.nav.dagpenger.saksbehandling.HendelseBehandler.Intern.Klage
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.OPPRETTET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.OppgaveTilstandslogg
import no.nav.dagpenger.saksbehandling.ReturnerTilSaksbehandlingÅrsak
import no.nav.dagpenger.saksbehandling.Sak
import no.nav.dagpenger.saksbehandling.TestHelper
import no.nav.dagpenger.saksbehandling.TestHelper.beslutter
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.db.DBTestHelper
import no.nav.dagpenger.saksbehandling.db.DBTestHelper.Companion.testPerson
import no.nav.dagpenger.saksbehandling.db.DatabaseSession
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.hendelser.AvbrytOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingTilGodkjenningHendelse
import no.nav.dagpenger.saksbehandling.hendelser.GodkjentBehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlageBehandlingUtført
import no.nav.dagpenger.saksbehandling.hendelser.KlageinstansVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.PåVentFristUtgåttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ReturnerTilSaksbehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SendTilKontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.TomHendelse
import no.nav.dagpenger.saksbehandling.hendelser.UtsettOppgaveHendelse
import no.nav.dagpenger.saksbehandling.klage.OpplysningType
import no.nav.dagpenger.saksbehandling.klage.UtfallType
import no.nav.dagpenger.saksbehandling.klage.Verdi
import no.nav.dagpenger.saksbehandling.klage.svarPåAlleOpplysningerMedUtfall
import no.nav.dagpenger.saksbehandling.statistikk.OppgaveITilstand
import no.nav.dagpenger.saksbehandling.statistikk.OppgaveITilstand.Tilstandsendring
import no.nav.dagpenger.saksbehandling.statistikk.db.SaksbehandlingsstatistikkRepository.Companion.ANTALL_PER_KJØRING
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

class PostgresSaksbehandlingsstatistikkRepositoryTest {
    @Test
    fun `Tilstandsendringer på oppgave utløst av Søknad skal oversendes saksbehandlingsstatistikk - Avbrutt manuelt`() {
        val behandling = TestHelper.lagBehandling()
        val oppgave =
            TestHelper.lagOppgave(
                behandling = behandling,
                tilstand = Oppgave.KlarTilBehandling,
                tilstandslogg =
                    OppgaveTilstandslogg().also {
                        it.leggTil(
                            nyTilstand = KLAR_TIL_BEHANDLING,
                            hendelse = TomHendelse,
                        )
                    },
            )
        val sak =
            Sak(
                opprettet = LocalDateTime.now(),
            )
        DBTestHelper.withMigratedDb { ds ->
            this.opprettSakMedBehandlingOgOppgave(
                person = testPerson,
                behandling = behandling,
                sak = sak,
                oppgave = oppgave,
                merkSomEgenSak = true,
            )
            val postgresStatistikkTjeneste = PostgresSaksbehandlingsstatistikkRepository(DatabaseSession(ds))
            val førsteTilstandsendring =
                postgresStatistikkTjeneste.oppgaveTilstandsendringer().let {
                    it.size shouldBe 1
                    val førsteTilstandsendring = it.single()
                    førsteTilstandsendring shouldBe
                        OppgaveITilstand(
                            oppgaveId = oppgave.oppgaveId,
                            mottatt = oppgave.opprettet,
                            sakId = sak.sakId,
                            behandlingId = behandling.behandlingId,
                            personIdent = testPerson.ident,
                            saksbehandlerIdent = null,
                            beslutterIdent = null,
                            versjon = Configuration.versjon,
                            tilstandsendring =
                                Tilstandsendring(
                                    sekvensnummer = 1,
                                    tilstandsendringId = oppgave.tilstandslogg.first().id,
                                    tilstand = "KLAR_TIL_BEHANDLING",
                                    tidspunkt = oppgave.tilstandslogg.first().tidspunkt,
                                ),
                            utløstAv = "SØKNAD",
                            behandlingResultat = null,
                            behandlingÅrsak = null,
                            fagsystem = "DAGPENGER",
                            arenaSakId = null,
                            resultatBegrunnelse = null,
                            relatertBehandlingId = null,
                        )
                    førsteTilstandsendring
                }

            postgresStatistikkTjeneste.markerTilstandsendringerSomOverført(førsteTilstandsendring.tilstandsendring.tilstandsendringId)
            postgresStatistikkTjeneste.oppgaveTilstandsendringer().size shouldBe 0

            oppgave.tildel(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    ansvarligIdent = TestHelper.saksbehandler.navIdent,
                    utførtAv = TestHelper.saksbehandler,
                ),
            )

            PostgresOppgaveRepository(DatabaseSession(ds)).lagre(oppgave)

            postgresStatistikkTjeneste.oppgaveTilstandsendringer().let {
                it.size shouldBe 1
                val tilstandsendring = it.single()
                tilstandsendring.tilstandsendring.tilstand shouldBe "UNDER_BEHANDLING"
                tilstandsendring.saksbehandlerIdent shouldBe TestHelper.saksbehandler.navIdent
                postgresStatistikkTjeneste.markerTilstandsendringerSomOverført(tilstandsendring.tilstandsendring.tilstandsendringId)
            }

            oppgave.utsett(
                utsettOppgaveHendelse =
                    UtsettOppgaveHendelse(
                        oppgaveId = oppgave.oppgaveId,
                        navIdent = TestHelper.saksbehandler.navIdent,
                        utsattTil =
                            java.time.LocalDate
                                .now()
                                .plusDays(1),
                        beholdOppgave = true,
                        årsak = Emneknagg.PåVent.AVVENT_MELDEKORT,
                        utførtAv = TestHelper.saksbehandler,
                    ),
            )

            oppgave.oppgaverPåVentMedUtgåttFrist(
                hendelse =
                    PåVentFristUtgåttHendelse(
                        oppgaveId = oppgave.oppgaveId,
                    ),
            )

            oppgave.sendTilKontroll(
                SendTilKontrollHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    utførtAv = TestHelper.saksbehandler,
                ),
            )

            oppgave.tildel(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    ansvarligIdent = beslutter.navIdent,
                    utførtAv = beslutter,
                ),
            )
            PostgresOppgaveRepository(DatabaseSession(ds)).lagre(oppgave)

            postgresStatistikkTjeneste.oppgaveTilstandsendringer().let {
                it.size shouldBe 4
                it[0].let { tilstandsendring ->
                    tilstandsendring.tilstandsendring.tilstand shouldBe "PAA_VENT"
                    tilstandsendring.beslutterIdent shouldBe null
                    tilstandsendring.saksbehandlerIdent shouldBe null
                    tilstandsendring.behandlingÅrsak shouldBe "AVVENT_MELDEKORT"
                    tilstandsendring.resultatBegrunnelse shouldBe null

                    postgresStatistikkTjeneste.markerTilstandsendringerSomOverført(tilstandsendring.tilstandsendring.tilstandsendringId)
                }
                it[1].let { tilstandsendring ->
                    tilstandsendring.tilstandsendring.tilstand shouldBe "UNDER_BEHANDLING"
                    tilstandsendring.beslutterIdent shouldBe null
                    tilstandsendring.saksbehandlerIdent shouldBe null
                    tilstandsendring.behandlingÅrsak shouldBe null
                    tilstandsendring.resultatBegrunnelse shouldBe null

                    postgresStatistikkTjeneste.markerTilstandsendringerSomOverført(tilstandsendring.tilstandsendring.tilstandsendringId)
                }
                it[2].let { tilstandsendring ->
                    tilstandsendring.tilstandsendring.tilstand shouldBe "KLAR_TIL_KONTROLL"
                    tilstandsendring.beslutterIdent shouldBe null
                    tilstandsendring.saksbehandlerIdent shouldBe null
                    tilstandsendring.behandlingÅrsak shouldBe null
                    tilstandsendring.resultatBegrunnelse shouldBe null

                    postgresStatistikkTjeneste.markerTilstandsendringerSomOverført(tilstandsendring.tilstandsendring.tilstandsendringId)
                }
                it[3].let { tilstandsendring ->
                    tilstandsendring.tilstandsendring.tilstand shouldBe "UNDER_KONTROLL"
                    tilstandsendring.beslutterIdent shouldBe beslutter.navIdent
                    tilstandsendring.saksbehandlerIdent shouldBe null
                    tilstandsendring.behandlingÅrsak shouldBe null
                    tilstandsendring.resultatBegrunnelse shouldBe null

                    postgresStatistikkTjeneste.markerTilstandsendringerSomOverført(tilstandsendring.tilstandsendring.tilstandsendringId)
                }
            }

            oppgave.returnerTilSaksbehandling(
                ReturnerTilSaksbehandlingHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    utførtAv = beslutter,
                    årsak = ReturnerTilSaksbehandlingÅrsak.FEIL_HJEMMEL,
                ),
            )
            oppgave.avbryt(
                AvbrytOppgaveHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    navIdent = TestHelper.saksbehandler.navIdent,
                    årsak = AVBRUTT_FLERE_SØKNADER,
                    utførtAv = TestHelper.saksbehandler,
                ),
            )
            PostgresOppgaveRepository(DatabaseSession(ds)).lagre(oppgave)

            postgresStatistikkTjeneste.oppgaveTilstandsendringer().let {
                it.size shouldBe 2
                it.first().let { tilstandsendring ->
                    tilstandsendring.tilstandsendring.tilstand shouldBe "UNDERKJENT_BESLUTTER"
                    tilstandsendring.beslutterIdent shouldBe null
                    tilstandsendring.saksbehandlerIdent shouldBe null
                    tilstandsendring.behandlingÅrsak shouldBe null
                    tilstandsendring.resultatBegrunnelse shouldBe "FEIL_HJEMMEL"
                }
                it.last().let { tilstandsendring ->
                    tilstandsendring.tilstandsendring.tilstand shouldBe "AVBRUTT_MANUELT"
                    tilstandsendring.beslutterIdent shouldBe null
                    tilstandsendring.saksbehandlerIdent shouldBe null
                    tilstandsendring.behandlingÅrsak shouldBe null
                    tilstandsendring.resultatBegrunnelse shouldBe "AVBRUTT_FLERE_SØKNADER"
                }
            }
        }
    }

    @Test
    fun `Tilstandsendringer på oppgave utløst av Søknad skal oversendes saksbehandlingsstatistikk - Ferdig behandlet`() {
        val behandling = TestHelper.lagBehandling()
        val oppgave =
            TestHelper.lagOppgave(
                behandling = behandling,
                tilstand = Oppgave.KlarTilBehandling,
                tilstandslogg =
                    OppgaveTilstandslogg().also {
                        it.leggTil(
                            nyTilstand = KLAR_TIL_BEHANDLING,
                            hendelse = TomHendelse,
                        )
                    },
            )
        val sak =
            Sak(
                opprettet = LocalDateTime.now(),
            )
        DBTestHelper.withMigratedDb { ds ->
            this.opprettSakMedBehandlingOgOppgave(
                person = testPerson,
                behandling = behandling,
                sak = sak,
                oppgave = oppgave,
                merkSomEgenSak = true,
            )
            val postgresStatistikkTjeneste = PostgresSaksbehandlingsstatistikkRepository(DatabaseSession(ds))
            val førsteTilstandsendring =
                postgresStatistikkTjeneste.oppgaveTilstandsendringer().let {
                    it.size shouldBe 1
                    val førsteTilstandsendring = it.single()
                    førsteTilstandsendring shouldBe
                        OppgaveITilstand(
                            oppgaveId = oppgave.oppgaveId,
                            mottatt = oppgave.opprettet,
                            sakId = sak.sakId,
                            behandlingId = behandling.behandlingId,
                            personIdent = testPerson.ident,
                            saksbehandlerIdent = null,
                            beslutterIdent = null,
                            versjon = Configuration.versjon,
                            tilstandsendring =
                                Tilstandsendring(
                                    sekvensnummer = 1,
                                    tilstandsendringId = oppgave.tilstandslogg.first().id,
                                    tilstand = "KLAR_TIL_BEHANDLING",
                                    tidspunkt = oppgave.tilstandslogg.first().tidspunkt,
                                ),
                            utløstAv = "SØKNAD",
                            behandlingResultat = null,
                            behandlingÅrsak = null,
                            fagsystem = "DAGPENGER",
                            arenaSakId = null,
                            resultatBegrunnelse = null,
                            relatertBehandlingId = null,
                        )
                    førsteTilstandsendring
                }

            postgresStatistikkTjeneste.oppgaveTilstandsendringerIkkeOverfort(ANTALL_PER_KJØRING).single() shouldBe førsteTilstandsendring
            postgresStatistikkTjeneste.markerTilstandsendringerSomOverført(førsteTilstandsendring.tilstandsendring.tilstandsendringId)
            postgresStatistikkTjeneste.oppgaveTilstandsendringer().size shouldBe 0

            oppgave.tildel(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    ansvarligIdent = TestHelper.saksbehandler.navIdent,
                    utførtAv = TestHelper.saksbehandler,
                ),
            )

            PostgresOppgaveRepository(DatabaseSession(ds)).lagre(oppgave)
            val andreTilstandsendring =
                postgresStatistikkTjeneste.oppgaveTilstandsendringer().let {
                    it.size shouldBe 1
                    val andreTilstandsendring = it.single()
                    andreTilstandsendring shouldBe
                        OppgaveITilstand(
                            oppgaveId = oppgave.oppgaveId,
                            mottatt = oppgave.opprettet,
                            sakId = sak.sakId,
                            behandlingId = behandling.behandlingId,
                            personIdent = testPerson.ident,
                            saksbehandlerIdent = TestHelper.saksbehandler.navIdent,
                            beslutterIdent = null,
                            versjon = Configuration.versjon,
                            tilstandsendring =
                                Tilstandsendring(
                                    sekvensnummer = 2,
                                    tilstandsendringId = oppgave.tilstandslogg.first().id,
                                    tilstand = "UNDER_BEHANDLING",
                                    tidspunkt = oppgave.tilstandslogg.first().tidspunkt,
                                ),
                            utløstAv = "SØKNAD",
                            behandlingResultat = null,
                            behandlingÅrsak = null,
                            fagsystem = "DAGPENGER",
                            arenaSakId = null,
                            resultatBegrunnelse = null,
                            relatertBehandlingId = null,
                        )
                    andreTilstandsendring
                }

            postgresStatistikkTjeneste.oppgaveTilstandsendringerIkkeOverfort(ANTALL_PER_KJØRING).single() shouldBe andreTilstandsendring
            postgresStatistikkTjeneste.markerTilstandsendringerSomOverført(andreTilstandsendring.tilstandsendring.tilstandsendringId)
            postgresStatistikkTjeneste.oppgaveTilstandsendringer().size shouldBe 0

            oppgave.ferdigstill(
                GodkjentBehandlingHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    meldingOmVedtakKilde = Oppgave.MeldingOmVedtakKilde.INGEN,
                    utførtAv = TestHelper.saksbehandler,
                ),
            )

            PostgresOppgaveRepository(DatabaseSession(ds)).lagre(oppgave)
            val tredjeTilstandsendring =
                postgresStatistikkTjeneste.oppgaveTilstandsendringer().let {
                    it.size shouldBe 1
                    val tredjeTilstandsendring = it.single()
                    tredjeTilstandsendring shouldBe
                        OppgaveITilstand(
                            oppgaveId = oppgave.oppgaveId,
                            mottatt = oppgave.opprettet,
                            sakId = sak.sakId,
                            behandlingId = behandling.behandlingId,
                            personIdent = testPerson.ident,
                            saksbehandlerIdent = null,
                            beslutterIdent = null,
                            versjon = Configuration.versjon,
                            tilstandsendring =
                                Tilstandsendring(
                                    sekvensnummer = 3,
                                    tilstandsendringId = oppgave.tilstandslogg.first().id,
                                    tilstand = "FERDIG_BEHANDLET",
                                    tidspunkt = oppgave.tilstandslogg.first().tidspunkt,
                                ),
                            utløstAv = "SØKNAD",
                            behandlingResultat = null,
                            behandlingÅrsak = null,
                            fagsystem = "DAGPENGER",
                            arenaSakId = null,
                            resultatBegrunnelse = null,
                            relatertBehandlingId = null,
                        )
                    tredjeTilstandsendring
                }

            postgresStatistikkTjeneste.oppgaveTilstandsendringerIkkeOverfort(ANTALL_PER_KJØRING).single() shouldBe tredjeTilstandsendring
            postgresStatistikkTjeneste.markerTilstandsendringerSomOverført(tredjeTilstandsendring.tilstandsendring.tilstandsendringId)
            postgresStatistikkTjeneste.oppgaveTilstandsendringer().size shouldBe 0
            postgresStatistikkTjeneste.oppgaveTilstandsendringerIkkeOverfort(ANTALL_PER_KJØRING).size shouldBe 0
        }
    }

    @Test
    fun `Tilstandsendringer på oppgave utløst av Innsending skal oversendes saksbehandlingsstatistikk`() {
        val innsendingBehandling = TestHelper.lagBehandling(utløstAvType = HendelseBehandler.Intern.Innsending)
        val innsendingOppgave =
            TestHelper.lagOppgave(
                behandling = innsendingBehandling,
                tilstand = Oppgave.KlarTilBehandling,
                tilstandslogg =
                    OppgaveTilstandslogg().also {
                        it.leggTil(
                            nyTilstand = KLAR_TIL_BEHANDLING,
                            hendelse = TomHendelse,
                        )
                    },
            )
        val sak =
            Sak(
                opprettet = LocalDateTime.now(),
            )
        DBTestHelper.withMigratedDb { ds ->
            this.opprettSakMedBehandlingOgOppgave(
                person = testPerson,
                behandling = innsendingBehandling,
                sak = sak,
                oppgave = innsendingOppgave,
                merkSomEgenSak = true,
            )
            val postgresStatistikkTjeneste = PostgresSaksbehandlingsstatistikkRepository(DatabaseSession(ds))
            postgresStatistikkTjeneste.oppgaveTilstandsendringer().size shouldBe 1

            innsendingOppgave.tildel(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = innsendingOppgave.oppgaveId,
                    ansvarligIdent = TestHelper.saksbehandler.navIdent,
                    utførtAv = TestHelper.saksbehandler,
                ),
            )

            PostgresOppgaveRepository(DatabaseSession(ds)).lagre(innsendingOppgave)

            // Hvert kall returnerer kun tilstandsendringer som ikke er vurdert før, derfor 1 og ikke 2.
            postgresStatistikkTjeneste.oppgaveTilstandsendringer().size shouldBe 1
            postgresStatistikkTjeneste.oppgaveTilstandsendringer().size shouldBe 0
        }
    }

    @Test
    fun `Tilstandsendringer på oppgave utløst av Klage skal oversendes saksbehandlingsstatistikk`() {
        val klageBehandling = TestHelper.lagKlageBehandling()
        val behandling =
            TestHelper.lagBehandling(
                behandlingId = klageBehandling.behandlingId,
                opprettet = klageBehandling.opprettet,
                utløstAvType = Klage,
            )
        val klageOppgave =
            TestHelper.lagOppgave(
                behandling = behandling,
                tilstand = Oppgave.KlarTilBehandling,
                tilstandslogg =
                    OppgaveTilstandslogg().also {
                        it.leggTil(
                            nyTilstand = KLAR_TIL_BEHANDLING,
                            hendelse = TomHendelse,
                        )
                    },
            )
        val sak = Sak(opprettet = LocalDateTime.now())

        DBTestHelper.withMigratedDb { ds ->
            this.opprettSakMedBehandlingOgOppgave(
                person = testPerson,
                behandling = behandling,
                sak = sak,
                oppgave = klageOppgave,
                merkSomEgenSak = true,
                klageBehandling = klageBehandling,
            )
            val postgresStatistikkTjeneste = PostgresSaksbehandlingsstatistikkRepository(DatabaseSession(ds))
            val førsteUttak = postgresStatistikkTjeneste.oppgaveTilstandsendringer()
            førsteUttak.size shouldBe 1
            førsteUttak.first().relatertBehandlingId shouldBe null

            klageOppgave.tildel(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = klageOppgave.oppgaveId,
                    ansvarligIdent = TestHelper.saksbehandler.navIdent,
                    utførtAv = TestHelper.saksbehandler,
                ),
            )

            PostgresOppgaveRepository(DatabaseSession(ds)).lagre(klageOppgave)

            // Hvert kall returnerer kun tilstandsendringer som ikke er vurdert før, derfor 1 og ikke 2.
            postgresStatistikkTjeneste.oppgaveTilstandsendringer().size shouldBe 1
            postgresStatistikkTjeneste.oppgaveTilstandsendringer().size shouldBe 0
        }
    }

    @Test
    fun `Tilstandsendringer på klage eldre enn klagegulvet skal ikke oversendes saksbehandlingsstatistikk`() {
        // Klager til og med denne id-en var i gang før eksporten av klage ble slått på, og ville derfor
        // manglet begynnelsen av historikken sin i datavarehuset. De holdes utenfor uttrekket.
        val klageEldreEnnGulvet = UUID.fromString("01a01292-a2da-70a7-9c0c-d0ddc1db3888")
        val klageBehandling = TestHelper.lagKlageBehandling(behandlingId = klageEldreEnnGulvet)
        val behandling =
            TestHelper.lagBehandling(
                behandlingId = klageBehandling.behandlingId,
                opprettet = klageBehandling.opprettet,
                utløstAvType = Klage,
            )
        val klageOppgave =
            TestHelper.lagOppgave(
                behandling = behandling,
                tilstand = Oppgave.KlarTilBehandling,
                tilstandslogg =
                    OppgaveTilstandslogg().also {
                        it.leggTil(
                            nyTilstand = KLAR_TIL_BEHANDLING,
                            hendelse = TomHendelse,
                        )
                    },
            )
        val sak = Sak(opprettet = LocalDateTime.now())

        DBTestHelper.withMigratedDb { ds ->
            this.opprettSakMedBehandlingOgOppgave(
                person = testPerson,
                behandling = behandling,
                sak = sak,
                oppgave = klageOppgave,
                merkSomEgenSak = true,
                klageBehandling = klageBehandling,
            )

            PostgresSaksbehandlingsstatistikkRepository(DatabaseSession(ds))
                .oppgaveTilstandsendringer()
                .shouldBeEmpty()
        }
    }

    @Test
    fun `Tilstandsendringer for opprettholdelse av klage skal spesialhåndteres`() {
        val klageBehandling = TestHelper.lagKlageBehandling()
        svarPåAlleOpplysningerMedUtfall(
            klageBehandling = klageBehandling,
            utfallType = UtfallType.OPPRETTHOLDELSE,
        )
        val behandling =
            TestHelper.lagBehandling(
                behandlingId = klageBehandling.behandlingId,
                opprettet = klageBehandling.opprettet,
                utløstAvType = Klage,
            )
        val klageOppgave =
            TestHelper.lagOppgave(
                behandling = behandling,
                tilstand = Oppgave.FerdigBehandlet,
                tilstandslogg =
                    OppgaveTilstandslogg().also {
                        it.leggTil(
                            nyTilstand = OPPRETTET,
                            hendelse = TomHendelse,
                        )
                        it.leggTil(
                            nyTilstand = KLAR_TIL_BEHANDLING,
                            hendelse = TomHendelse,
                        )
                        it.leggTil(
                            nyTilstand = UNDER_BEHANDLING,
                            hendelse = TomHendelse,
                        )
                        it.leggTil(
                            nyTilstand = FERDIG_BEHANDLET,
                            hendelse =
                                KlageBehandlingUtført(
                                    behandlingId = klageBehandling.behandlingId,
                                    utførtAv = TestHelper.saksbehandler,
                                ),
                        )
                        it.leggTil(
                            nyTilstand = FERDIG_BEHANDLET,
                            hendelse =
                                KlageinstansVedtakHendelse(
                                    type = KlageinstansVedtakHendelse.KlageinstansVedtakType.KLAGE,
                                    klageId = klageBehandling.behandlingId,
                                    klageinstansVedtakId = UUIDv7.ny(),
                                    avsluttet = LocalDateTime.now(),
                                    utfall = "STADFESTELSE",
                                    journalpostIder = listOf("123"),
                                ),
                        )
                    },
            )
        val sak = Sak(opprettet = LocalDateTime.now())

        DBTestHelper.withMigratedDb { ds ->
            this.opprettSakMedBehandlingOgOppgave(
                person = testPerson,
                behandling = behandling,
                sak = sak,
                oppgave = klageOppgave,
                merkSomEgenSak = true,
                klageBehandling = klageBehandling,
            )
            val påklagetVedtakVerdi: Verdi =
                klageBehandling
                    .alleOpplysninger()
                    .single { it.type == OpplysningType.KLAGEN_GJELDER_VEDTAK }
                    .verdi()
            require(påklagetVedtakVerdi is Verdi.UUID) { "Påklaget vedtak må være av typen UUID" }
            ds.insertBehandlingIEksisterendeSak(
                personId = testPerson.id,
                sakId = sak.sakId,
                behandlingId = påklagetVedtakVerdi.value,
            )
            val postgresStatistikkTjeneste = PostgresSaksbehandlingsstatistikkRepository(DatabaseSession(ds))
            val oppgaveTilstandsendringer = postgresStatistikkTjeneste.oppgaveTilstandsendringer()
            oppgaveTilstandsendringer.size shouldBe 5
            oppgaveTilstandsendringer[2].tilstandsendring.tilstand shouldBe "UNDER_BEHANDLING"
            oppgaveTilstandsendringer[3].tilstandsendring.tilstand shouldBe "OVERSENDT_KLAGEINSTANS"
            oppgaveTilstandsendringer[3].relatertBehandlingId shouldBe påklagetVedtakVerdi.value
            oppgaveTilstandsendringer[3].behandlingResultat shouldBe "Opprettholdelse"
            oppgaveTilstandsendringer[4].tilstandsendring.tilstand shouldBe "FERDIG_BEHANDLET"
            oppgaveTilstandsendringer[4].relatertBehandlingId shouldBe påklagetVedtakVerdi.value
            oppgaveTilstandsendringer[4].behandlingResultat shouldBe "STADFESTELSE"
        }
    }

    @Test
    fun `Oppgave returnert maskinelt fra kontroll til saksbehandler skal oversendes statistikk som RETURNERT_MASKINELT`() {
        val behandling = TestHelper.lagBehandling()
        val oppgave =
            TestHelper.lagOppgave(
                behandling = behandling,
                tilstand = Oppgave.KlarTilBehandling,
                tilstandslogg =
                    OppgaveTilstandslogg().also {
                        it.leggTil(
                            nyTilstand = KLAR_TIL_BEHANDLING,
                            hendelse = TomHendelse,
                        )
                    },
            )
        val sak = Sak(opprettet = LocalDateTime.now())
        DBTestHelper.withMigratedDb { ds ->
            this.opprettSakMedBehandlingOgOppgave(
                person = testPerson,
                behandling = behandling,
                sak = sak,
                oppgave = oppgave,
                merkSomEgenSak = true,
            )
            val postgresStatistikkTjeneste = PostgresSaksbehandlingsstatistikkRepository(DatabaseSession(ds))

            oppgave.tildel(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    ansvarligIdent = TestHelper.saksbehandler.navIdent,
                    utførtAv = TestHelper.saksbehandler,
                ),
            )
            oppgave.sendTilKontroll(
                SendTilKontrollHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    utførtAv = TestHelper.saksbehandler,
                ),
            )
            oppgave.tildel(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    ansvarligIdent = beslutter.navIdent,
                    utførtAv = beslutter,
                ),
            )
            PostgresOppgaveRepository(DatabaseSession(ds)).lagre(oppgave)

            postgresStatistikkTjeneste.oppgaveTilstandsendringer().forEach {
                postgresStatistikkTjeneste.markerTilstandsendringerSomOverført(it.tilstandsendring.tilstandsendringId)
            }

            oppgave.behandlingTilGodkjenning(
                BehandlingTilGodkjenningHendelse(
                    behandlingId = behandling.behandlingId,
                    ident = testPerson.ident,
                ),
            )
            PostgresOppgaveRepository(DatabaseSession(ds)).lagre(oppgave)

            postgresStatistikkTjeneste.oppgaveTilstandsendringer().let {
                it.size shouldBe 1
                val tilstandsendring = it.single()
                tilstandsendring.tilstandsendring.tilstand shouldBe "RETURNERT_MASKINELT"
                tilstandsendring.saksbehandlerIdent shouldBe null
                tilstandsendring.beslutterIdent shouldBe null
            }
        }
    }

    private fun DataSource.insertBehandlingIEksisterendeSak(
        personId: UUID,
        sakId: UUID,
        behandlingId: UUID = UUIDv7.ny(),
        opprettet: LocalDateTime = LocalDateTime.now(),
        utløstAv: String = "MELDEKORT",
    ) = sessionOf(this).use { session ->
        session.run(
            action =
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        INSERT INTO behandling_v1
                        ( id, person_id, sak_id, opprettet, utlost_av )
                        VALUES
                        ( :behandling_id, :person_id, :sak_id, :opprettet, :utlost_av )
                        ON CONFLICT DO NOTHING 
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "behandling_id" to behandlingId,
                            "person_id" to personId,
                            "sak_id" to sakId,
                            "opprettet" to opprettet,
                            "utlost_av" to utløstAv,
                        ),
                ).asUpdate,
        )
    }
}
