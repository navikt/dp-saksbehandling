package no.nav.dagpenger.saksbehandling.statistikk

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.OppgaveTilstandslogg
import no.nav.dagpenger.saksbehandling.Sak
import no.nav.dagpenger.saksbehandling.TestHelper
import no.nav.dagpenger.saksbehandling.db.DBTestHelper
import no.nav.dagpenger.saksbehandling.hendelser.TomHendelse
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class PostgresStatistikkTjenesteTest {
    @Test
    fun `Skal hente ferdigbehandlede oppgaver som tilhører dp-sak og som ikke er oversendt statistikk`() {
        val dpSakFerdigBehandletOppgaveId1 = UUID.randomUUID()
        val dpSakAvbruttOppgaveId2 = UUID.randomUUID()
        val dpSakUnderBehandlingOppgaveId3 = UUID.randomUUID()
        val arenaFerdigBehandletOppgaveId4 = UUID.randomUUID()
        DBTestHelper.withMigratedDb { ds ->
            TestHelper.lagBehandling().let {
                opprettSakMedBehandlingOgOppgave(
                    person = DBTestHelper.testPerson,
                    sak =
                        Sak(
                            søknadId = DBTestHelper.søknadId,
                            opprettet = LocalDateTime.now(),
                        ),
                    behandling = it,
                    oppgave =
                        TestHelper.lagOppgave(
                            oppgaveId = dpSakFerdigBehandletOppgaveId1,
                            behandling = it,
                            tilstand = Oppgave.FerdigBehandlet,
                            tilstandslogg =
                                OppgaveTilstandslogg().also { logg ->
                                    logg.leggTil(
                                        nyTilstand = Oppgave.Tilstand.Type.FERDIG_BEHANDLET,
                                        hendelse = TomHendelse,
                                    )
                                },
                        ),
                    merkSomEgenSak = true,
                )
            }

            TestHelper.lagBehandling().let {
                opprettSakMedBehandlingOgOppgave(
                    person = DBTestHelper.testPerson,
                    sak =
                        Sak(
                            søknadId = DBTestHelper.søknadId,
                            opprettet = LocalDateTime.now(),
                        ),
                    behandling = it,
                    oppgave =
                        TestHelper.lagOppgave(
                            oppgaveId = dpSakAvbruttOppgaveId2,
                            behandling = it,
                            tilstand = Oppgave.Avbrutt,
                            tilstandslogg =
                                OppgaveTilstandslogg().also { logg ->
                                    logg.leggTil(
                                        nyTilstand = Oppgave.Tilstand.Type.AVBRUTT,
                                        hendelse = TomHendelse,
                                    )
                                },
                        ),
                    merkSomEgenSak = true,
                )
            }

            TestHelper.lagBehandling().let {
                opprettSakMedBehandlingOgOppgave(
                    person = DBTestHelper.testPerson,
                    sak =
                        Sak(
                            søknadId = DBTestHelper.søknadId,
                            opprettet = LocalDateTime.now(),
                        ),
                    behandling = it,
                    oppgave =
                        TestHelper.lagOppgave(
                            oppgaveId = dpSakUnderBehandlingOppgaveId3,
                            behandling = it,
                            tilstand = Oppgave.UnderBehandling,
                        ),
                    merkSomEgenSak = true,
                )
            }

            TestHelper.lagBehandling().let {
                opprettSakMedBehandlingOgOppgave(
                    person = DBTestHelper.testPerson,
                    sak =
                        Sak(
                            søknadId = DBTestHelper.søknadId,
                            opprettet = LocalDateTime.now(),
                        ),
                    behandling = it,
                    oppgave =
                        TestHelper.lagOppgave(
                            oppgaveId = arenaFerdigBehandletOppgaveId4,
                            behandling = it,
                            tilstand = Oppgave.FerdigBehandlet,
                        ),
                    merkSomEgenSak = false,
                )
            }

            val postgresStatistikkTjeneste = PostgresStatistikkTjeneste(ds)

            val oppgaverSomSkalSendesTilDVH = postgresStatistikkTjeneste.oppgaverTilStatistikk()
            oppgaverSomSkalSendesTilDVH.map { it }.toSet() shouldBe
                setOf(
                    dpSakFerdigBehandletOppgaveId1,
                    dpSakAvbruttOppgaveId2,
                )

            oppgaverSomSkalSendesTilDVH.forEach {
                postgresStatistikkTjeneste.markerOppgaveTilStatistikkSomOverført(it) shouldBe 1
            }
            postgresStatistikkTjeneste.oppgaverTilStatistikk() shouldBe emptyList()

            val dpSakFerdigBehandletOppgaveId5 = UUID.randomUUID()
            TestHelper.lagBehandling().let {
                opprettSakMedBehandlingOgOppgave(
                    person = DBTestHelper.testPerson,
                    sak =
                        Sak(
                            søknadId = DBTestHelper.søknadId,
                            opprettet = LocalDateTime.now(),
                        ),
                    behandling = it,
                    oppgave =
                        TestHelper.lagOppgave(
                            oppgaveId = dpSakFerdigBehandletOppgaveId5,
                            behandling = it,
                            tilstand = Oppgave.FerdigBehandlet,
                            tilstandslogg =
                                OppgaveTilstandslogg().also { logg ->
                                    logg.leggTil(
                                        nyTilstand = Oppgave.Tilstand.Type.FERDIG_BEHANDLET,
                                        hendelse = TomHendelse,
                                    )
                                },
                        ),
                    merkSomEgenSak = true,
                )
            }

            val nyOppgaveSomSkalSendesTilDVH = postgresStatistikkTjeneste.oppgaverTilStatistikk()
            nyOppgaveSomSkalSendesTilDVH.map { it }.toSet() shouldBe setOf(dpSakFerdigBehandletOppgaveId5)
        }
    }
}
