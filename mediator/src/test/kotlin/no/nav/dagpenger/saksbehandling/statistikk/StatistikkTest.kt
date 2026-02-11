package no.nav.dagpenger.saksbehandling.statistikk

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.TestHelper.lagBehandling
import no.nav.dagpenger.saksbehandling.TestHelper.lagOppgave
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.db.DBTestHelper
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.statistikk.db.PostgresSaksbehandlingsstatistikkRepository
import org.junit.jupiter.api.Test
import javax.sql.DataSource

class StatistikkTest {
    @Test
    fun `test hentAntallVedtakGjort`() {
        val behandling1 = lagBehandling(behandlingId = UUIDv7.ny())
        val behandling2 = lagBehandling(behandlingId = UUIDv7.ny())
        val behandling3 = lagBehandling(behandlingId = UUIDv7.ny())
        DBTestHelper.withBehandlinger(
            behandlinger = listOf(behandling1, behandling2, behandling3),
        ) { ds: DataSource ->
            // Insert test data
            val repo = PostgresOppgaveRepository(ds)
            repo.lagre(
                lagOppgave(
                    oppgaveId = UUIDv7.ny(),
                    tilstand = Oppgave.FerdigBehandlet,
                    behandling = behandling1,
                ),
            )
            repo.lagre(
                lagOppgave(
                    oppgaveId = UUIDv7.ny(),
                    tilstand = Oppgave.FerdigBehandlet,
                    behandling = behandling2,
                ),
            )
            repo.lagre(
                lagOppgave(
                    oppgaveId = UUIDv7.ny(),
                    tilstand = Oppgave.FerdigBehandlet,
                    behandling = behandling3,
                ),
            )

            val statistikkTjeneste = PostgresSaksbehandlingsstatistikkRepository(ds)
            val result = statistikkTjeneste.hentAntallVedtakGjort()

            result.dag shouldBe 3
            result.uke shouldBe 3
            result.totalt shouldBe 3
        }
    }

    @Test
    fun `test hentBeholdningsInfo`() {
        val behandling1 = lagBehandling()
        val behandling2 = lagBehandling()
        DBTestHelper.withBehandlinger(
            behandlinger = listOf(behandling1, behandling2),
        ) { ds: DataSource ->
            // Insert test data
            val repo = PostgresOppgaveRepository(ds)
            repo.lagre(lagOppgave(tilstand = Oppgave.KlarTilBehandling, behandling = behandling1))
            repo.lagre(lagOppgave(tilstand = Oppgave.KlarTilBehandling, behandling = behandling2))

            val statistikkTjeneste = PostgresSaksbehandlingsstatistikkRepository(ds)
            val result = statistikkTjeneste.hentBeholdningsInfo()

            result.antallOppgaverKlarTilBehandling shouldBe 2
        }
    }
}
