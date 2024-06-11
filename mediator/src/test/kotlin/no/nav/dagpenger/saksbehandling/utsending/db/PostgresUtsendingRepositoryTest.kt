package no.nav.dagpenger.saksbehandling.utsending.db

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.Sak
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.db.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.db.lagOppgave
import no.nav.dagpenger.saksbehandling.utsending.Utsending
import org.junit.jupiter.api.Test
import java.util.UUID
import javax.sql.DataSource

class PostgresUtsendingRepositoryTest {
    @Test
    fun `lagring og henting av utsending`() {
        withMigratedDb { ds ->

            val oppgaveId = lagreOppgaveOgBehandling(ds)
            val sak = Sak("id", "fagsystem")

            val repository = PostgresUtsendingRepository(ds)
            val utsending = Utsending(oppgaveId = oppgaveId, sak = sak)
            repository.lagre(utsending)

            repository.hent(utsending.oppgaveId) shouldBe utsending

            repository.finnUtsendingFor(UUID.randomUUID()) shouldBe null
            shouldThrow<UtsendingIkkeFunnet> {
                repository.hent(UUID.randomUUID())
            }
        }
    }

    private fun lagreOppgaveOgBehandling(dataSource: DataSource): UUID {
        val oppgave = lagOppgave()
        val repository = PostgresOppgaveRepository(dataSource)
        repository.lagre(oppgave)
        return oppgave.oppgaveId
    }
}
