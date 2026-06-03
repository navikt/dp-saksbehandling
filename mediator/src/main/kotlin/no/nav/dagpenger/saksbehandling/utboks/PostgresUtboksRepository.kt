package no.nav.dagpenger.saksbehandling.utboks

import kotliquery.queryOf
import no.nav.dagpenger.saksbehandling.db.DatabaseSession
import no.nav.dagpenger.saksbehandling.db.Transaksjonskontekst
import java.time.LocalDateTime

class PostgresUtboksRepository(
    private val databaseSession: DatabaseSession,
) : UtboksRepository {
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
                    statement = "INSERT INTO kafka_utboks_v1 (key, message, status) VALUES (:key, :message, :status)",
                    paramMap = mapOf("key" to key, "message" to message, "status" to tilstand),
                ).asUpdate,
            )
        }
    }

    override fun hentOgTellMedTilstand(
        tilstand: String,
        limit: Int,
    ): Pair<List<UtboksMelding>, Int> =
        databaseSession.session { session ->
            val rader =
                session.run(
                    queryOf(
                        //language=PostgreSQL
                        statement =
                            """
                            SELECT id, key, message, status, COUNT(*) OVER() AS totalt_antall
                            FROM kafka_utboks_v1
                            WHERE status = :status
                            ORDER BY id
                            LIMIT :limit
                            """.trimIndent(),
                        paramMap = mapOf("status" to tilstand, "limit" to limit),
                    ).map { row ->
                        UtboksMelding(
                            id = row.long("id"),
                            key = row.string("key"),
                            message = row.string("message"),
                            tilstand = row.string("status"),
                        ) to row.int("totalt_antall")
                    }.asList,
                )
            rader.map { it.first } to (rader.firstOrNull()?.second ?: 0)
        }

    override fun oppdaterTilstand(
        id: Long,
        tilstand: String,
    ) {
        databaseSession.session { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement = "UPDATE kafka_utboks_v1 SET status = :status WHERE id = :id",
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
                    statement = "DELETE FROM kafka_utboks_v1 WHERE status = :status AND registrert_tidspunkt < :cutoff",
                    paramMap = mapOf("status" to tilstand, "cutoff" to cutoff),
                ).asUpdate,
            )
        }
}
