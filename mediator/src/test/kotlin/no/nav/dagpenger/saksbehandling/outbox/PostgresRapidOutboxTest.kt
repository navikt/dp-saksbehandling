package no.nav.dagpenger.saksbehandling.outbox

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import com.github.navikt.tbd_libs.rapids_and_rivers_api.OutgoingMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.db.DBTestHelper
import no.nav.dagpenger.saksbehandling.db.DatabaseSession
import no.nav.dagpenger.saksbehandling.db.Transaksjoner
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDateTime
import javax.sql.DataSource

class PostgresRapidOutboxTest {
    @Test
    fun `send skriver til outbox-tabell i samme transaksjon`() {
        DBTestHelper.withMigratedDb { ds ->
            val transaksjoner = Transaksjoner(DatabaseSession(ds))
            val tjeneste = outbox(ds, TestRapid())

            transaksjoner.transaksjon { ctx ->
                tjeneste.send(key = "12345678901", message = """{"@event_name":"test"}""", ctx = ctx)
            }

            pendingCount(ds) shouldBe 1
        }
    }

    @Test
    fun `send ruller tilbake ved feil i samme transaksjon`() {
        DBTestHelper.withMigratedDb { ds ->
            val transaksjoner = Transaksjoner(DatabaseSession(ds))
            val tjeneste = outbox(ds, TestRapid())

            runCatching {
                transaksjoner.transaksjon { ctx ->
                    tjeneste.send(key = "12345678901", message = """{"@event_name":"test"}""", ctx = ctx)
                    error("Simulert feil etter send")
                }
            }

            antall(ds) shouldBe 0
        }
    }

    @Test
    fun `publiserVentende publiserer PENDING records i FIFO-rekkefølge og markerer som SENDT`() {
        DBTestHelper.withMigratedDb { ds ->
            val rapid = TestRapid()
            val transaksjoner = Transaksjoner(DatabaseSession(ds))
            val tjeneste = outbox(ds, rapid)

            transaksjoner.transaksjon { ctx ->
                tjeneste.send("a", """{"ident":"a"}""", ctx)
                tjeneste.send("b", """{"ident":"b"}""", ctx)
                tjeneste.send("c", """{"ident":"c"}""", ctx)
            }

            tjeneste.publiserVentende()

            rapid.inspektør.size shouldBe 3
            alleHarStatus(ds, "SENDT") shouldBe true
        }
    }

    @Test
    fun `publiserVentende stopper ved første feil og lar resten stå PENDING`() {
        DBTestHelper.withMigratedDb { ds ->
            val transaksjoner = Transaksjoner(DatabaseSession(ds))
            val tjeneste = outbox(ds, FailOnFirstPublishRapid())

            transaksjoner.transaksjon { ctx ->
                tjeneste.send("x", """{"ident":"x"}""", ctx)
                tjeneste.send("y", """{"ident":"y"}""", ctx)
            }

            tjeneste.publiserVentende()

            pendingCount(ds) shouldBe 2
        }
    }

    @Test
    fun `publiserVentende publiserer ikke når ingen PENDING records finnes`() {
        DBTestHelper.withMigratedDb { ds ->
            val rapid = TestRapid()
            outbox(ds, rapid).publiserVentende()
            rapid.inspektør.size shouldBe 0
        }
    }

    @Test
    fun `slettGamleSendte rydder bort sendte records eldre enn levetiden`() {
        DBTestHelper.withMigratedDb { ds ->
            val rapid = TestRapid()
            val transaksjoner = Transaksjoner(DatabaseSession(ds))
            val tjeneste = outbox(ds, rapid, levetidSendte = Duration.ofDays(7))

            transaksjoner.transaksjon { ctx ->
                tjeneste.send("gammel", """{"ident":"g"}""", ctx)
            }
            tjeneste.publiserVentende()
            settCreatedAtPåAlle(ds, LocalDateTime.now().minusDays(10))

            tjeneste.slettGamleSendte()

            antall(ds) shouldBe 0
        }
    }

    private fun outbox(
        ds: DataSource,
        rapid: RapidsConnection,
        levetidSendte: Duration = Duration.ofDays(7),
    ) = PostgresRapidOutbox(
        repository = PostgresOutboxRepository(DatabaseSession(ds)),
        rapidsConnection = rapid,
        levetidSendte = levetidSendte,
    )

    private fun antall(ds: DataSource): Long =
        sessionOf(ds).use { session ->
            session.single(queryOf("SELECT COUNT(*) FROM outbox")) { it.long(1) }!!
        }

    private fun pendingCount(ds: DataSource): Long =
        sessionOf(ds).use { session ->
            session.single(queryOf("SELECT COUNT(*) FROM outbox WHERE status = 'PENDING'")) { it.long(1) }!!
        }

    private fun alleHarStatus(
        ds: DataSource,
        status: String,
    ): Boolean =
        sessionOf(ds).use { session ->
            session.single(
                queryOf("SELECT COUNT(*) FROM outbox WHERE status != :status", mapOf("status" to status)),
            ) { it.long(1) } == 0L
        }

    private fun settCreatedAtPåAlle(
        ds: DataSource,
        tidspunkt: LocalDateTime,
    ) {
        sessionOf(ds).use { session ->
            session.run(
                queryOf("UPDATE outbox SET created_at = :tid", mapOf("tid" to tidspunkt)).asUpdate,
            )
        }
    }
}

private class FailOnFirstPublishRapid : RapidsConnection() {
    override fun publish(message: String) = throw RuntimeException("Simulert Kafka-feil")

    override fun publish(
        key: String,
        message: String,
    ) = throw RuntimeException("Simulert Kafka-feil")

    override fun publish(messages: List<OutgoingMessage>) = throw RuntimeException("Simulert Kafka-feil")

    override fun rapidName(): String = "FailOnFirstPublishRapid"

    override fun start() {}

    override fun stop() {}
}
