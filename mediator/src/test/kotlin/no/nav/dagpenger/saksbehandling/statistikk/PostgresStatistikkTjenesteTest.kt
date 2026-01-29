package no.nav.dagpenger.saksbehandling.statistikk

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.Configuration
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.OppgaveTilstandslogg
import no.nav.dagpenger.saksbehandling.Sak
import no.nav.dagpenger.saksbehandling.TestHelper
import no.nav.dagpenger.saksbehandling.db.DBTestHelper
import no.nav.dagpenger.saksbehandling.db.DBTestHelper.Companion.testPerson
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.hendelser.SendTilKontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.TomHendelse
import no.nav.dagpenger.saksbehandling.statistikk.OppgaveTilstandsendring.StatistikkOppgaveTilstandsendring
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class PostgresStatistikkTjenesteTest {
    @Test
    fun hubba() {
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
                sak =
                sak,
                oppgave =
                oppgave,
                merkSomEgenSak = true,
            )
            val postgresStatistikkTjeneste = PostgresStatistikkTjeneste(dataSource = ds)
            val førsteTilstandsendring =
                postgresStatistikkTjeneste.oppgaveTilstandsendringer().let {
                    it.size shouldBe 1
                    val førsteTilstandsendring = it.single()
                    førsteTilstandsendring shouldBe
                        OppgaveTilstandsendring(
                            oppgaveId = oppgave.oppgaveId,
                            mottatt = oppgave.opprettet.toLocalDate(),
                            sakId = sak.sakId,
                            behandlingId = behandling.behandlingId,
                            personIdent = testPerson.ident,
                            saksbehandlerIdent = null,
                            beslutterIdent = null,
                            versjon = Configuration.versjon,
                            tilstandsendring =
                                StatistikkOppgaveTilstandsendring(
                                    id = oppgave.tilstandslogg.first().id,
                                    tilstand = "KLAR_TIL_BEHANDLING",
                                    tidspunkt = oppgave.tilstandslogg.first().tidspunkt,
                                ),
                            utløstAv = "SØKNAD",
                        )
                    førsteTilstandsendring
                }

            postgresStatistikkTjeneste.markerTilstandsendringerSomOverført(førsteTilstandsendring.tilstandsendring.id)
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
                postgresStatistikkTjeneste.markerTilstandsendringerSomOverført(tilstandsendring.tilstandsendring.id)
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
                    tilstandsendring.tilstandsendring.tilstand shouldBe "UNDER_KONTROLL"
                    tilstandsendring.beslutterIdent shouldBe TestHelper.beslutter.navIdent
                    tilstandsendring.saksbehandlerIdent shouldBe null
                }
                it.last().let { tilstandsendring ->
                    tilstandsendring.tilstandsendring.tilstand shouldBe "KLAR_TIL_KONTROLL"
                    tilstandsendring.beslutterIdent shouldBe null
                    tilstandsendring.saksbehandlerIdent shouldBe null
                }
            }
        }
    }
}
