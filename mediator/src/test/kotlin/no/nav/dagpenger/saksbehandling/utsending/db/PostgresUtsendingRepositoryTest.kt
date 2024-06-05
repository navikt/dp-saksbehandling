package no.nav.dagpenger.saksbehandling.utsending.db

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.db.PostgresOppgaveRepository
import no.nav.dagpenger.saksbehandling.db.lagBehandling
import no.nav.dagpenger.saksbehandling.db.lagOppgave
import no.nav.dagpenger.saksbehandling.utsending.Utsending
import org.junit.jupiter.api.Test
import java.util.UUID
import javax.sql.DataSource

class PostgresUtsendingRepositoryTest {
    @Test
    fun `lagring og henting av utsending`() {
        withMigratedDb { ds ->

            val (oppgaveId, behandlingId) = lagreOppgaveOgBehandling(ds)

            val repository = PostgresUtsendingRepository(ds)
            val utsending = Utsending(oppgaveId)
            repository.lagre(utsending)

            repository.hent(utsending.oppgaveId) shouldBe utsending
            repository.hentUtsendingFor(behandlingId) shouldBe utsending

            repository.finnUtsendingFor(UUID.randomUUID()) shouldBe null
            shouldThrow<UtsendingIkkeFunnet> {
                repository.hent(UUID.randomUUID())
            }
        }
    }

    private fun lagreOppgaveOgBehandling(dataSource: DataSource): Pair<UUID, UUID> {
        val behandling = lagBehandling()
        val oppgave = lagOppgave(behandling = behandling)
        val repository = PostgresOppgaveRepository(dataSource)
        repository.lagre(oppgave)
        return Pair(oppgave.oppgaveId, behandling.behandlingId)
    }
}
