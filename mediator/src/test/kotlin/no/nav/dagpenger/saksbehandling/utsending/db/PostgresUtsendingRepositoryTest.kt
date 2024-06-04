package no.nav.dagpenger.saksbehandling.utsending.db

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.db.PostgresRepository
import no.nav.dagpenger.saksbehandling.db.lagOppgave
import no.nav.dagpenger.saksbehandling.utsending.Utsending
import org.junit.jupiter.api.Test
import java.util.UUID
import javax.sql.DataSource

class PostgresUtsendingRepositoryTest {
    @Test
    fun `lagring og henting av utsending`() {
        withMigratedDb { ds ->
            val oppgaveId = lagreOppgave(ds)
            val repository = PostgresUtsendingRepository(ds)
            val utsending = Utsending(oppgaveId)
            repository.lagre(utsending)
            val hentetUtsending = repository.hent(utsending.oppgaveId)
            hentetUtsending shouldBe utsending
        }
    }

    private fun lagreOppgave(dataSource: DataSource): UUID {
        val oppgave = lagOppgave()
        val repository = PostgresRepository(dataSource)
        repository.lagre(oppgave)
        return oppgave.oppgaveId
    }
}
