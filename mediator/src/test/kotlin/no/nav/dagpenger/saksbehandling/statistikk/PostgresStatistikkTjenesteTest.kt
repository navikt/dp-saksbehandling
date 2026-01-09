package no.nav.dagpenger.saksbehandling.statistikk

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Sak
import no.nav.dagpenger.saksbehandling.TestHelper
import no.nav.dagpenger.saksbehandling.db.DBTestHelper
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class PostgresStatistikkTjenesteTest {
    @Test
    fun `Skal hente ferdigbehandlede oppgaver som tilhører dp-sak og som ikke er oversendt statistikk`() {
        val dpSakFerdigBehandletOppgave1 = UUID.randomUUID()
        val dpSakFerdigBehandletOppgave2 = UUID.randomUUID()
        val dpSakUnderBehandlingOppgave3 = UUID.randomUUID()
        val arenaFerdigBehandletOppgave4 = UUID.randomUUID()
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
                            oppgaveId = dpSakFerdigBehandletOppgave1,
                            behandling = it,
                            tilstand = Oppgave.FerdigBehandlet,
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
                            oppgaveId = dpSakFerdigBehandletOppgave2,
                            behandling = it,
                            tilstand = Oppgave.FerdigBehandlet,
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
                            oppgaveId = dpSakUnderBehandlingOppgave3,
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
                            oppgaveId = arenaFerdigBehandletOppgave4,
                            behandling = it,
                            tilstand = Oppgave.FerdigBehandlet,
                        ),
                    merkSomEgenSak = false,
                )
            }

            val postgresStatistikkTjeneste = PostgresStatistikkTjeneste(ds)

            val oppgaverSomSkalSendesTilDVH = postgresStatistikkTjeneste.oppgaverTilStatistikk()
            oppgaverSomSkalSendesTilDVH.map { it.first }.toSet() shouldBe setOf(dpSakFerdigBehandletOppgave1, dpSakFerdigBehandletOppgave2)

            oppgaverSomSkalSendesTilDVH.forEach {
                postgresStatistikkTjeneste.markerOppgaveTilStatistikkSomOverført(it.first) shouldBe 1
            }
            postgresStatistikkTjeneste.oppgaverTilStatistikk() shouldBe emptyList()

            val dpSakFerdigBehandletOppgave5 = UUID.randomUUID()
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
                            oppgaveId = dpSakFerdigBehandletOppgave5,
                            behandling = it,
                            tilstand = Oppgave.FerdigBehandlet,
                        ),
                    merkSomEgenSak = true,
                )
            }

            val nyOppgaveSomSkalSendesTilDVH = postgresStatistikkTjeneste.oppgaverTilStatistikk()
            nyOppgaveSomSkalSendesTilDVH.map { it.first }.toSet() shouldBe setOf(dpSakFerdigBehandletOppgave5)
        }
    }
}
