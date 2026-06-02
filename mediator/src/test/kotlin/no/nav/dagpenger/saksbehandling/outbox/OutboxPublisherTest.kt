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
import javax.sql.DataSource

class OutboxPublisherTest {
    @Test
    fun `Publiserer PENDING records i FIFO-rekkefølge`() {
        DBTestHelper.withMigratedDb { ds ->
            val rapid = TestRapid()
            val transaksjoner = Transaksjoner(DatabaseSession(ds))
            val outbox = PostgresOutbox()

            transaksjoner.transaksjon { ctx ->
                outbox.send("a", """{"ident":"a"}""", ctx)
                outbox.send("b", """{"ident":"b"}""", ctx)
                outbox.send("c", """{"ident":"c"}""", ctx)
            }

            OutboxPublisher(ds, rapid).publiser()

            rapid.inspektør.size shouldBe 3
            alleHarStatus(ds, "SENDT") shouldBe true
        }
    }

    @Test
    fun `Stopper ved første feil og lar resten stå PENDING`() {
        DBTestHelper.withMigratedDb { ds ->
            val transaksjoner = Transaksjoner(DatabaseSession(ds))
            val outbox = PostgresOutbox()

            transaksjoner.transaksjon { ctx ->
                outbox.send("x", """{"ident":"x"}""", ctx)
                outbox.send("y", """{"ident":"y"}""", ctx)
            }

            OutboxPublisher(ds, FailOnFirstPublishRapid()).publiser()

            pendingCount(ds) shouldBe 2
        }
    }

    @Test
    fun `Publiserer ikke når ingen PENDING records finnes`() {
        DBTestHelper.withMigratedDb { ds ->
            val rapid = TestRapid()
            OutboxPublisher(ds, rapid).publiser()
            rapid.inspektør.size shouldBe 0
        }
    }

    private fun pendingCount(ds: DataSource): Long =
        sessionOf(ds).use { session ->
            session.single(
                queryOf("SELECT COUNT(*) FROM outbox WHERE status = 'PENDING'"),
            ) { it.long(1) }!!
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
