package no.nav.dagpenger.saksbehandling.outbox

import kotliquery.queryOf
import no.nav.dagpenger.saksbehandling.db.DatabaseSession
import no.nav.dagpenger.saksbehandling.db.Transaksjonskontekst
import java.time.LocalDateTime

class PostgresOutboxRepository(
    private val databaseSession: DatabaseSession,
) : OutboxRepository {
    override fun lagre(
        key: String,
        message: String,
        ctx: Transaksjonskontekst.Aktiv,
    ) {
        databaseSession.inContext(ctx) {
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement = "INSERT INTO outbox (key, message) VALUES (:key, :message)",
                    paramMap = mapOf("key" to key, "message" to message),
                ).asUpdate,
            )
        }
    }

    override fun hentPending(limit: Int): List<OutboxRecord> =
        databaseSession.session { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement = "SELECT id, key, message FROM outbox WHERE status = 'PENDING' ORDER BY id LIMIT :limit",
                    paramMap = mapOf("limit" to limit),
                ).map { row ->
                    OutboxRecord(
                        id = row.long("id"),
                        key = row.string("key"),
                        message = row.string("message"),
                    )
                }.asList,
            )
        }

    override fun markerSendt(id: Long) {
        databaseSession.session { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement = "UPDATE outbox SET status = 'SENDT' WHERE id = :id",
                    paramMap = mapOf("id" to id),
                ).asUpdate,
            )
        }
    }

    override fun slettSendteEldreEnn(cutoff: LocalDateTime): Int =
        databaseSession.session { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement = "DELETE FROM outbox WHERE status = 'SENDT' AND created_at < :cutoff",
                    paramMap = mapOf("cutoff" to cutoff),
                ).asUpdate,
            )
        }
}
