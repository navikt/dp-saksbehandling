package no.nav.dagpenger.saksbehandling.statistikk

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.db.DBTestHelper
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.lagBehandling
import no.nav.dagpenger.saksbehandling.lagOppgave
import org.junit.jupiter.api.Test
import javax.sql.DataSource

class StatistikkTest {
    @Test
    fun `test hentAntallVedtakGjort`() {
        val behandlingId1 = UUIDv7.ny()
        val behandlingId2 = UUIDv7.ny()
        val behandlingId3 = UUIDv7.ny()
        DBTestHelper.withBehandlinger(
            behandlinger =
                listOf(
                    lagBehandling(behandlingId1),
                    lagBehandling(behandlingId2),
                    lagBehandling(behandlingId3),
                ),
        ) { ds: DataSource ->
            // Insert test data
            val repo = PostgresOppgaveRepository(ds)
            repo.lagre(lagOppgave(tilstand = Oppgave.FerdigBehandlet, behandlingId = behandlingId1))
            repo.lagre(lagOppgave(tilstand = Oppgave.FerdigBehandlet, behandlingId = behandlingId2))
            repo.lagre(lagOppgave(tilstand = Oppgave.FerdigBehandlet, behandlingId = behandlingId3))

            val statistikkTjeneste = PostgresStatistikkTjeneste(ds)
            val result = statistikkTjeneste.hentAntallVedtakGjort()

            result.dag shouldBe 3
            result.uke shouldBe 3
            result.totalt shouldBe 3
        }
    }

    @Test
    fun `test hentBeholdningsInfo`() {
        val behandlingId1 = UUIDv7.ny()
        val behandlingId2 = UUIDv7.ny()
        DBTestHelper.withBehandlinger(
            behandlinger =
                listOf(
                    lagBehandling(behandlingId1),
                    lagBehandling(behandlingId2),
                ),
        ) { ds: DataSource ->
            // Insert test data
            val repo = PostgresOppgaveRepository(ds)
            repo.lagre(lagOppgave(tilstand = Oppgave.KlarTilBehandling, behandlingId = behandlingId1))
            repo.lagre(lagOppgave(tilstand = Oppgave.KlarTilBehandling, behandlingId = behandlingId2))

            val statistikkTjeneste = PostgresStatistikkTjeneste(ds)
            val result = statistikkTjeneste.hentBeholdningsInfo()

            result.antallOppgaverKlarTilBehandling shouldBe 2
        }
    }
}
