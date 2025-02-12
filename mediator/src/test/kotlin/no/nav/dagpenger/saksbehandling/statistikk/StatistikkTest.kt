package no.nav.dagpenger.saksbehandling.statistikk

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.lagOppgave
import org.junit.jupiter.api.Test
import javax.sql.DataSource

class StatistikkTest {
    @Test
    fun `test hentAntallVedtakGjort`() {
        withMigratedDb { ds: DataSource ->
            // Insert test data
            val repo = PostgresOppgaveRepository(ds)
            repo.lagre(lagOppgave(tilstand = Oppgave.FerdigBehandlet))
            repo.lagre(lagOppgave(tilstand = Oppgave.FerdigBehandlet))
            repo.lagre(lagOppgave(tilstand = Oppgave.FerdigBehandlet))

            val statistikkTjeneste = PostgresStatistikkTjeneste(ds)
            val result = statistikkTjeneste.hentAntallVedtakGjort()

            result.dag shouldBe 3
            result.uke shouldBe 3
            result.totalt shouldBe 3
        }
    }
}
