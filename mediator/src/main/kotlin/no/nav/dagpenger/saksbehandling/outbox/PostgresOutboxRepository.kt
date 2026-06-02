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
        tilstand: String,
        ctx: Transaksjonskontekst.Aktiv,
    ) {
        databaseSession.inContext(ctx) {
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement = "INSERT INTO outbox (key, message, status) VALUES (:key, :message, :status)",
                    paramMap = mapOf("key" to key, "message" to message, "status" to tilstand),
                ).asUpdate,
            )
        }
    }

    override fun hentMedTilstand(
        tilstand: String,
        limit: Int,
    ): List<OutboxRecord> =
        databaseSession.session { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement = "SELECT id, key, message FROM outbox WHERE status = :status ORDER BY id LIMIT :limit",
                    paramMap = mapOf("status" to tilstand, "limit" to limit),
                ).map { row ->
                    OutboxRecord(
                        id = row.long("id"),
                        key = row.string("key"),
                        message = row.string("message"),
                    )
                }.asList,
            )
        }

    override fun oppdaterTilstand(
        id: Long,
        tilstand: String,
    ) {
        databaseSession.session { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement = "UPDATE outbox SET status = :status WHERE id = :id",
                    paramMap = mapOf("status" to tilstand, "id" to id),
                ).asUpdate,
            )
        }
    }

    override fun slettMedTilstandEldreEnn(
        tilstand: String,
        cutoff: LocalDateTime,
    ): Int =
        databaseSession.session { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement = "DELETE FROM outbox WHERE status = :status AND created_at < :cutoff",
                    paramMap = mapOf("status" to tilstand, "cutoff" to cutoff),
                ).asUpdate,
            )
        }
}
