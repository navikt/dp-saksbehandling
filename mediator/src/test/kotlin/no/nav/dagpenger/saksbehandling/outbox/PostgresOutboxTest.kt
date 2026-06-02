package no.nav.dagpenger.saksbehandling.outbox

import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.db.DBTestHelper
import no.nav.dagpenger.saksbehandling.db.DatabaseSession
import no.nav.dagpenger.saksbehandling.db.Transaksjoner
import org.junit.jupiter.api.Test

class PostgresOutboxTest {
    @Test
    fun `send skriver til outbox-tabell i samme transaksjon`() {
        DBTestHelper.withMigratedDb { ds ->
            val transaksjoner = Transaksjoner(DatabaseSession(ds))
            val outbox = PostgresOutbox(PostgresOutboxRepository(ds))

            transaksjoner.transaksjon { ctx ->
                outbox.send(key = "12345678901", message = """{"@event_name":"test"}""", ctx = ctx)
            }

            val records =
                sessionOf(ds).use { session ->
                    session.run(
                        queryOf("SELECT key, message, status FROM outbox ORDER BY id")
                            .map { row ->
                                Triple(row.string("key"), row.string("message"), row.string("status"))
                            }.asList,
                    )
                }

            records.size shouldBe 1
            records[0].first shouldBe "12345678901"
            records[0].third shouldBe "PENDING"
        }
    }

    @Test
    fun `send ruller tilbake ved feil i samme transaksjon`() {
        DBTestHelper.withMigratedDb { ds ->
            val transaksjoner = Transaksjoner(DatabaseSession(ds))
            val outbox = PostgresOutbox(PostgresOutboxRepository(ds))

            runCatching {
                transaksjoner.transaksjon { ctx ->
                    outbox.send(key = "12345678901", message = """{"@event_name":"test"}""", ctx = ctx)
                    error("Simulert feil etter outbox.send")
                }
            }

            val count =
                sessionOf(ds).use { session ->
                    session.run(queryOf("SELECT count(*) FROM outbox").map { it.int(1) }.asSingle)
                }

            count shouldBe 0
        }
    }
}
