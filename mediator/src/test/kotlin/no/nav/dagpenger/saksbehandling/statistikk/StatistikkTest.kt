package no.nav.dagpenger.saksbehandling.statistikk

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.RettTilDagpengerOppgave
import no.nav.dagpenger.saksbehandling.TestHelper.lagRettTilDPBehandling
import no.nav.dagpenger.saksbehandling.TestHelper.lagRettTilDPOppgave
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.db.DBTestHelper
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import org.junit.jupiter.api.Test
import javax.sql.DataSource

class StatistikkTest {
    @Test
    fun `test hentAntallVedtakGjort`() {
        val behandling1 = lagRettTilDPBehandling(behandlingId = UUIDv7.ny())
        val behandling2 = lagRettTilDPBehandling(behandlingId = UUIDv7.ny())
        val behandling3 = lagRettTilDPBehandling(behandlingId = UUIDv7.ny())
        DBTestHelper.withBehandlinger(
            behandlinger = listOf(behandling1, behandling2, behandling3),
        ) { ds: DataSource ->
            // Insert test data
            val repo = PostgresOppgaveRepository(ds)
            repo.lagre(
                lagRettTilDPOppgave(
                    oppgaveId = UUIDv7.ny(),
                    tilstand = RettTilDagpengerOppgave.FerdigBehandlet,
                    behandling = behandling1,
                ),
            )
            repo.lagre(
                lagRettTilDPOppgave(
                    oppgaveId = UUIDv7.ny(),
                    tilstand = RettTilDagpengerOppgave.FerdigBehandlet,
                    behandling = behandling2,
                ),
            )
            repo.lagre(
                lagRettTilDPOppgave(
                    oppgaveId = UUIDv7.ny(),
                    tilstand = RettTilDagpengerOppgave.FerdigBehandlet,
                    behandling = behandling3,
                ),
            )

            val statistikkTjeneste = PostgresStatistikkTjeneste(ds)
            val result = statistikkTjeneste.hentAntallVedtakGjort()

            result.dag shouldBe 3
            result.uke shouldBe 3
            result.totalt shouldBe 3
        }
    }

    @Test
    fun `test hentBeholdningsInfo`() {
        val behandling1 = lagRettTilDPBehandling()
        val behandling2 = lagRettTilDPBehandling()
        DBTestHelper.withBehandlinger(
            behandlinger = listOf(behandling1, behandling2),
        ) { ds: DataSource ->
            // Insert test data
            val repo = PostgresOppgaveRepository(ds)
            repo.lagre(lagRettTilDPOppgave(tilstand = RettTilDagpengerOppgave.KlarTilBehandling, behandling = behandling1))
            repo.lagre(lagRettTilDPOppgave(tilstand = RettTilDagpengerOppgave.KlarTilBehandling, behandling = behandling2))

            val statistikkTjeneste = PostgresStatistikkTjeneste(ds)
            val result = statistikkTjeneste.hentBeholdningsInfo()

            result.antallOppgaverKlarTilBehandling shouldBe 2
        }
    }
}
