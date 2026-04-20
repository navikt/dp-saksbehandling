package no.nav.dagpenger.saksbehandling.statistikk.db

import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import no.nav.dagpenger.saksbehandling.Configuration
import no.nav.dagpenger.saksbehandling.Emneknagg
import no.nav.dagpenger.saksbehandling.Emneknagg.AvbrytBehandling.AVBRUTT_FLERE_SØKNADER
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.OppgaveTilstandslogg
import no.nav.dagpenger.saksbehandling.ReturnerTilSaksbehandlingÅrsak
import no.nav.dagpenger.saksbehandling.Sak
import no.nav.dagpenger.saksbehandling.TestHelper
import no.nav.dagpenger.saksbehandling.TestHelper.beslutter
import no.nav.dagpenger.saksbehandling.UtløstAvType
import no.nav.dagpenger.saksbehandling.db.DBTestHelper
import no.nav.dagpenger.saksbehandling.db.DBTestHelper.Companion.testPerson
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.hendelser.AvbrytOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.GodkjentBehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.PåVentFristUtgåttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ReturnerTilSaksbehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SendTilKontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.TomHendelse
import no.nav.dagpenger.saksbehandling.hendelser.UtsettOppgaveHendelse
import no.nav.dagpenger.saksbehandling.statistikk.OppgaveITilstand
import no.nav.dagpenger.saksbehandling.statistikk.OppgaveITilstand.Tilstandsendring
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

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
                            nyTilstand = Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING,
                            hendelse = TomHendelse,
                        )
                    },
            )
        val sak =
            Sak(
                søknadId = DBTestHelper.søknadId,
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
            val postgresStatistikkTjeneste = PostgresSaksbehandlingsstatistikkRepository(dataSource = ds)
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

            PostgresOppgaveRepository(dataSource = ds).lagre(oppgave)

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
            PostgresOppgaveRepository(dataSource = ds).lagre(oppgave)

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
            PostgresOppgaveRepository(dataSource = ds).lagre(oppgave)

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
                            nyTilstand = Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING,
                            hendelse = TomHendelse,
                        )
                    },
            )
        val sak =
            Sak(
                søknadId = DBTestHelper.søknadId,
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
            val postgresStatistikkTjeneste = PostgresSaksbehandlingsstatistikkRepository(dataSource = ds)
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

            PostgresOppgaveRepository(dataSource = ds).lagre(oppgave)
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
                        )
                    andreTilstandsendring
                }
            postgresStatistikkTjeneste.markerTilstandsendringerSomOverført(andreTilstandsendring.tilstandsendring.tilstandsendringId)
            postgresStatistikkTjeneste.oppgaveTilstandsendringer().size shouldBe 0

            oppgave.ferdigstill(
                GodkjentBehandlingHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    meldingOmVedtakKilde = Oppgave.MeldingOmVedtakKilde.INGEN,
                    utførtAv = TestHelper.saksbehandler,
                ),
            )

            PostgresOppgaveRepository(dataSource = ds).lagre(oppgave)
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
                        )
                    tredjeTilstandsendring
                }
            postgresStatistikkTjeneste.markerTilstandsendringerSomOverført(tredjeTilstandsendring.tilstandsendring.tilstandsendringId)
            postgresStatistikkTjeneste.oppgaveTilstandsendringer().size shouldBe 0
        }
    }

    @Test
    fun `Tilstandsendringer på oppgave utløst av Innsending skal oversendes saksbehandlingsstatistikk`() {
        val innsendingBehandling = TestHelper.lagBehandling(utløstAvType = UtløstAvType.Intern.Innsending)
        val innsendingOppgave =
            TestHelper.lagOppgave(
                behandling = innsendingBehandling,
                tilstand = Oppgave.KlarTilBehandling,
                tilstandslogg =
                    OppgaveTilstandslogg().also {
                        it.leggTil(
                            nyTilstand = Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING,
                            hendelse = TomHendelse,
                        )
                    },
            )
        val sak =
            Sak(
                søknadId = DBTestHelper.søknadId,
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
            val postgresStatistikkTjeneste = PostgresSaksbehandlingsstatistikkRepository(dataSource = ds)
            postgresStatistikkTjeneste.oppgaveTilstandsendringer().size shouldBe 1

            innsendingOppgave.tildel(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = innsendingOppgave.oppgaveId,
                    ansvarligIdent = TestHelper.saksbehandler.navIdent,
                    utførtAv = TestHelper.saksbehandler,
                ),
            )

            PostgresOppgaveRepository(dataSource = ds).lagre(innsendingOppgave)

            postgresStatistikkTjeneste.oppgaveTilstandsendringer().size shouldBe 2
        }
    }

    @Test
    fun `Tilstandsendringer på oppgave utløst av Klage skal ikke oversendes saksbehandlingsstatistikk`() {
        val klageBehandling = TestHelper.lagBehandling(utløstAvType = UtløstAvType.Intern.Klage)
        val klageOppgave =
            TestHelper.lagOppgave(
                behandling = klageBehandling,
                tilstand = Oppgave.KlarTilBehandling,
                tilstandslogg =
                    OppgaveTilstandslogg().also {
                        it.leggTil(
                            nyTilstand = Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING,
                            hendelse = TomHendelse,
                        )
                    },
            )
        val sak =
            Sak(
                søknadId = DBTestHelper.søknadId,
                opprettet = LocalDateTime.now(),
            )
        DBTestHelper.withMigratedDb { ds ->
            this.opprettSakMedBehandlingOgOppgave(
                person = testPerson,
                behandling = klageBehandling,
                sak = sak,
                oppgave = klageOppgave,
                merkSomEgenSak = true,
            )
            val postgresStatistikkTjeneste = PostgresSaksbehandlingsstatistikkRepository(dataSource = ds)
            postgresStatistikkTjeneste.oppgaveTilstandsendringer().size shouldBe 0

            klageOppgave.tildel(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = klageOppgave.oppgaveId,
                    ansvarligIdent = TestHelper.saksbehandler.navIdent,
                    utførtAv = TestHelper.saksbehandler,
                ),
            )

            PostgresOppgaveRepository(dataSource = ds).lagre(klageOppgave)

            postgresStatistikkTjeneste.oppgaveTilstandsendringer().size shouldBe 0
        }
    }
}
