package no.nav.dagpenger.saksbehandling.statistikk

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.Configuration
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.OppgaveTilstandslogg
import no.nav.dagpenger.saksbehandling.Sak
import no.nav.dagpenger.saksbehandling.TestHelper
import no.nav.dagpenger.saksbehandling.UtløstAvType
import no.nav.dagpenger.saksbehandling.db.DBTestHelper
import no.nav.dagpenger.saksbehandling.db.DBTestHelper.Companion.testPerson
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.hendelser.SendTilKontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.TomHendelse
import no.nav.dagpenger.saksbehandling.statistikk.OppgaveITilstand.Tilstandsendring
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class PostgresStatistikkTjenesteTest {
    @Test
    fun `Tilstandsendringer på oppgave utløst av Søknad skal oversendes saksbehandlingsstatistikk`() {
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
            val postgresStatistikkTjeneste = PostgresStatistikkTjeneste(dataSource = ds)
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

            oppgave.sendTilKontroll(
                SendTilKontrollHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    utførtAv = TestHelper.saksbehandler,
                ),
            )

            oppgave.tildel(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    ansvarligIdent = TestHelper.beslutter.navIdent,
                    utførtAv = TestHelper.beslutter,
                ),
            )

            PostgresOppgaveRepository(dataSource = ds).lagre(oppgave)
            postgresStatistikkTjeneste.oppgaveTilstandsendringer().let {
                it.size shouldBe 2
                it.first().let { tilstandsendring ->
                    tilstandsendring.tilstandsendring.tilstand shouldBe "KLAR_TIL_KONTROLL"
                    tilstandsendring.beslutterIdent shouldBe null
                    tilstandsendring.saksbehandlerIdent shouldBe null
                }
                it.last().let { tilstandsendring ->
                    tilstandsendring.tilstandsendring.tilstand shouldBe "UNDER_KONTROLL"
                    tilstandsendring.beslutterIdent shouldBe TestHelper.beslutter.navIdent
                    tilstandsendring.saksbehandlerIdent shouldBe null
                }
            }
        }
    }

    @Test
    fun `Tilstandsendringer på oppgave utløst av Innsending skal oversendes saksbehandlingsstatistikk`() {
        val innsendingBehandling = TestHelper.lagBehandling(utløstAvType = UtløstAvType.INNSENDING)
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
            val postgresStatistikkTjeneste = PostgresStatistikkTjeneste(dataSource = ds)
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
        val klageBehandling = TestHelper.lagBehandling(utløstAvType = UtløstAvType.KLAGE)
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
            val postgresStatistikkTjeneste = PostgresStatistikkTjeneste(dataSource = ds)
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
