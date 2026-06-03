package no.nav.dagpenger.saksbehandling.utboks

import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.db.DBTestHelper
import no.nav.dagpenger.saksbehandling.db.DatabaseSession
import no.nav.dagpenger.saksbehandling.db.Transaksjoner
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import javax.sql.DataSource

class PostgresUtboksRepositoryTest {
    private val pending = UtboksTilstand.PENDING.name
    private val sendt = UtboksTilstand.SENDT.name

    @Test
    fun `lagre skriver melding i delt transaksjon`() {
        DBTestHelper.withMigratedDb { ds ->
            val transaksjoner = Transaksjoner(DatabaseSession(ds))
            val repository = PostgresUtboksRepository(DatabaseSession(ds))

            transaksjoner.transaksjon { ctx ->
                repository.lagre(key = "123", message = """{"a":1}""", tilstand = pending, ctx = ctx)
            }

            repository.hentMedTilstand(pending).size shouldBe 1
        }
    }

    @Test
    fun `lagre ruller tilbake når transaksjonen feiler`() {
        DBTestHelper.withMigratedDb { ds ->
            val transaksjoner = Transaksjoner(DatabaseSession(ds))
            val repository = PostgresUtboksRepository(DatabaseSession(ds))

            runCatching {
                transaksjoner.transaksjon { ctx ->
                    repository.lagre(key = "123", message = """{"a":1}""", tilstand = pending, ctx = ctx)
                    error("feil")
                }
            }

            repository.hentMedTilstand(pending).size shouldBe 0
        }
    }

    @Test
    fun `hentMedTilstand returnerer i FIFO-rekkefølge og respekterer limit`() {
        DBTestHelper.withMigratedDb { ds ->
            val transaksjoner = Transaksjoner(DatabaseSession(ds))
            val repository = PostgresUtboksRepository(DatabaseSession(ds))

            transaksjoner.transaksjon { ctx ->
                repository.lagre("a", """{"i":"a"}""", pending, ctx)
                repository.lagre("b", """{"i":"b"}""", pending, ctx)
                repository.lagre("c", """{"i":"c"}""", pending, ctx)
            }

            val meldinger = repository.hentMedTilstand(pending, limit = 2)
            meldinger.size shouldBe 2
            meldinger.map { it.key } shouldBe listOf("a", "b")
            meldinger.map { it.tilstand } shouldBe listOf(pending, pending)
        }
    }

    @Test
    fun `hentMedTilstand filtrerer på tilstand`() {
        DBTestHelper.withMigratedDb { ds ->
            val transaksjoner = Transaksjoner(DatabaseSession(ds))
            val repository = PostgresUtboksRepository(DatabaseSession(ds))

            transaksjoner.transaksjon { ctx ->
                repository.lagre("a", """{"i":"a"}""", pending, ctx)
                repository.lagre("b", """{"i":"b"}""", pending, ctx)
            }
            val b = repository.hentMedTilstand(pending).first { it.key == "b" }
            repository.oppdaterTilstand(b.id, sendt)

            repository.hentMedTilstand(pending).map { it.key } shouldBe listOf("a")
            repository.hentMedTilstand(sendt).map { it.key } shouldBe listOf("b")
        }
    }

    @Test
    fun `oppdaterTilstand setter ny tilstand`() {
        DBTestHelper.withMigratedDb { ds ->
            val transaksjoner = Transaksjoner(DatabaseSession(ds))
            val repository = PostgresUtboksRepository(DatabaseSession(ds))

            transaksjoner.transaksjon { ctx ->
                repository.lagre("a", """{"i":"a"}""", pending, ctx)
            }
            val melding = repository.hentMedTilstand(pending).single()

            repository.oppdaterTilstand(melding.id, sendt)

            repository.hentMedTilstand(pending).size shouldBe 0
            statusFor(ds, melding.id) shouldBe sendt
        }
    }

    @Test
    fun `slettMedTilstandEldreEnn sletter kun gamle meldinger med angitt tilstand`() {
        DBTestHelper.withMigratedDb { ds ->
            val transaksjoner = Transaksjoner(DatabaseSession(ds))
            val repository = PostgresUtboksRepository(DatabaseSession(ds))

            transaksjoner.transaksjon { ctx ->
                repository.lagre("gammel", """{"i":"g"}""", pending, ctx)
                repository.lagre("ny", """{"i":"n"}""", pending, ctx)
            }
            val meldinger = repository.hentMedTilstand(pending)
            val gammel = meldinger.first { it.key == "gammel" }
            val ny = meldinger.first { it.key == "ny" }
            repository.oppdaterTilstand(gammel.id, sendt)
            repository.oppdaterTilstand(ny.id, sendt)
            settCreatedAt(ds, gammel.id, LocalDateTime.now().minusDays(10))

            val slettet = repository.slettMedTilstandEldreEnn(sendt, LocalDateTime.now().minusDays(7))

            slettet shouldBe 1
            statusFor(ds, ny.id) shouldBe sendt
            statusFor(ds, gammel.id) shouldBe null
        }
    }

    private fun statusFor(
        ds: DataSource,
        id: Long,
    ): String? =
        sessionOf(ds).use { session ->
            session.run(
                queryOf("SELECT status FROM kafka_utboks_v1 WHERE id = :id", mapOf("id" to id))
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
                    "UPDATE kafka_utboks_v1 SET registrert_tidspunkt = :tid WHERE id = :id",
                    mapOf("tid" to tidspunkt, "id" to id),
                ).asUpdate,
            )
        }
    }
}
