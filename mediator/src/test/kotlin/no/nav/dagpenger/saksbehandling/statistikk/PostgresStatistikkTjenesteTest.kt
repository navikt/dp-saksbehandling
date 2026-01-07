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
    fun `Skal hente ut oppgaver som tilhører dp-sak og som ikke er oversendt til DVH`() {
        val dpSakOppgave1 = UUID.randomUUID()
        val dpSakOppgave2 = UUID.randomUUID()
        val arenaOppgave1 = UUID.randomUUID()
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
                            oppgaveId = dpSakOppgave1,
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
                            oppgaveId = dpSakOppgave2,
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
                            oppgaveId = arenaOppgave1,
                            behandling = it,
                            tilstand = Oppgave.FerdigBehandlet,
                        ),
                    merkSomEgenSak = false,
                )
            }

            val postgresStatistikkTjeneste = PostgresStatistikkTjeneste(ds)

            val oppgaverSomSkalSendesTilDVH = postgresStatistikkTjeneste.hentOppgaver()
            oppgaverSomSkalSendesTilDVH.map { it.first }.toSet() shouldBe setOf(dpSakOppgave1, dpSakOppgave2)

            postgresStatistikkTjeneste.oppdaterOppgaver(oppgaverSomSkalSendesTilDVH) shouldBe 2
            postgresStatistikkTjeneste.hentOppgaver() shouldBe emptyList()
        }
    }
}
