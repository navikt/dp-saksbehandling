package no.nav.dagpenger.saksbehandling.utsending.db

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.Sak
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.hendelser.lagreOppgaveOgBehandling
import no.nav.dagpenger.saksbehandling.utsending.Utsending
import org.junit.jupiter.api.Test
import java.util.UUID

class PostgresUtsendingRepositoryTest {
    @Test
    fun `lagring og henting av utsending`() {
        withMigratedDb { ds ->

            val oppgaveId = lagreOppgaveOgBehandling(ds).first
            val sak = Sak("id", "fagsystem")

            val repository = PostgresUtsendingRepository(ds)
            val distribusjonId = "distribusjonId"
            val utsending = Utsending(oppgaveId = oppgaveId, sak = sak, distribusjonId = distribusjonId)
            repository.lagre(utsending)

            repository.hent(utsending.oppgaveId) shouldBe utsending

            repository.finnUtsendingFor(UUID.randomUUID()) shouldBe null
            shouldThrow<UtsendingIkkeFunnet> {
                repository.hent(UUID.randomUUID())
            }
        }
    }

}
