package no.nav.dagpenger.saksbehandling.outbox

import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.db.DBTestHelper
import no.nav.dagpenger.saksbehandling.db.DatabaseSession
import no.nav.dagpenger.saksbehandling.db.Transaksjoner
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import javax.sql.DataSource

class OutboxRepositoryTest {
    @Test
    fun `lagre skriver record i delt transaksjon`() {
        DBTestHelper.withMigratedDb { ds ->
            val transaksjoner = Transaksjoner(DatabaseSession(ds))
            val repository = PostgresOutboxRepository(DatabaseSession(ds))

            transaksjoner.transaksjon { ctx ->
                repository.lagre(key = "123", message = """{"a":1}""", ctx = ctx)
            }

            repository.hentPending().size shouldBe 1
        }
    }

    @Test
    fun `lagre ruller tilbake når transaksjonen feiler`() {
        DBTestHelper.withMigratedDb { ds ->
            val transaksjoner = Transaksjoner(DatabaseSession(ds))
            val repository = PostgresOutboxRepository(DatabaseSession(ds))

            runCatching {
                transaksjoner.transaksjon { ctx ->
                    repository.lagre(key = "123", message = """{"a":1}""", ctx = ctx)
                    error("feil")
                }
            }

            repository.hentPending().size shouldBe 0
        }
    }

    @Test
    fun `hentPending returnerer i FIFO-rekkefølge og respekterer limit`() {
        DBTestHelper.withMigratedDb { ds ->
            val transaksjoner = Transaksjoner(DatabaseSession(ds))
            val repository = PostgresOutboxRepository(DatabaseSession(ds))

            transaksjoner.transaksjon { ctx ->
                repository.lagre("a", """{"i":"a"}""", ctx)
                repository.lagre("b", """{"i":"b"}""", ctx)
                repository.lagre("c", """{"i":"c"}""", ctx)
            }

            val pending = repository.hentPending(limit = 2)
            pending.size shouldBe 2
            pending.map { it.key } shouldBe listOf("a", "b")
        }
    }

    @Test
    fun `markerSendt setter status til SENDT`() {
        DBTestHelper.withMigratedDb { ds ->
            val transaksjoner = Transaksjoner(DatabaseSession(ds))
            val repository = PostgresOutboxRepository(DatabaseSession(ds))

            transaksjoner.transaksjon { ctx ->
                repository.lagre("a", """{"i":"a"}""", ctx)
            }
            val record = repository.hentPending().single()

            repository.markerSendt(record.id)

            repository.hentPending().size shouldBe 0
            statusFor(ds, record.id) shouldBe "SENDT"
        }
    }

    @Test
    fun `slettSendteEldreEnn sletter kun gamle SENDT-records`() {
        DBTestHelper.withMigratedDb { ds ->
            val transaksjoner = Transaksjoner(DatabaseSession(ds))
            val repository = PostgresOutboxRepository(DatabaseSession(ds))

            transaksjoner.transaksjon { ctx ->
                repository.lagre("gammel", """{"i":"g"}""", ctx)
                repository.lagre("ny", """{"i":"n"}""", ctx)
            }
            val records = repository.hentPending()
            val gammel = records.first { it.key == "gammel" }
            val ny = records.first { it.key == "ny" }
            repository.markerSendt(gammel.id)
            repository.markerSendt(ny.id)
            settCreatedAt(ds, gammel.id, LocalDateTime.now().minusDays(10))

            val slettet = repository.slettSendteEldreEnn(LocalDateTime.now().minusDays(7))

            slettet shouldBe 1
            statusFor(ds, ny.id) shouldBe "SENDT"
            statusFor(ds, gammel.id) shouldBe null
        }
    }

    private fun statusFor(
        ds: DataSource,
        id: Long,
    ): String? =
        sessionOf(ds).use { session ->
            session.run(
                queryOf("SELECT status FROM outbox WHERE id = :id", mapOf("id" to id))
                    .map { it.string("status") }
                    .asSingle,
            )
        }

    private fun settCreatedAt(
        ds: DataSource,
        id: Long,
        tidspunkt: LocalDateTime,
    ) {
        sessionOf(ds).use { session ->
            session.run(
                queryOf(
                    "UPDATE outbox SET created_at = :tid WHERE id = :id",
                    mapOf("tid" to tidspunkt, "id" to id),
                ).asUpdate,
            )
        }
    }
}
