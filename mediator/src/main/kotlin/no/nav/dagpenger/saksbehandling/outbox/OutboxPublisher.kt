package no.nav.dagpenger.saksbehandling.outbox

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.queryOf
import kotliquery.sessionOf
import javax.sql.DataSource

/**
 * Leser PENDING-rader fra outbox-tabellen og publiserer dem til Rapids & Rivers.
 *
 * Garantier:
 * - Global FIFO (ORDER BY id)
 * - Stopper ved første feil — retry ved neste poll
 * - fnr (key) og meldingsinnhold logges kun til sikkerlogg (GDPR)
 */
class OutboxPublisher(
    private val dataSource: DataSource,
    private val rapidsConnection: RapidsConnection,
) {
    private val logger = KotlinLogging.logger {}
    private val sikkerlogg = KotlinLogging.logger("tjenestekall")

    fun publiser() {
        sessionOf(dataSource).use { session ->
            val records =
                session.list(
                    queryOf(
                        //language=PostgreSQL
                        "SELECT id, key, message FROM outbox WHERE status = 'PENDING' ORDER BY id LIMIT 100",
                    ),
                ) { row -> OutboxRecord(row.long("id"), row.string("key"), row.string("message")) }
            for (record in records) {
                try {
                    rapidsConnection.publish(record.key, record.message)
                    session.run(
                        queryOf(
                            //language=PostgreSQL
                            "UPDATE outbox SET status = 'SENDT' WHERE id = :id",
                            mapOf("id" to record.id),
                        ).asUpdate,
                    )
                    logger.info { "Publiserte outbox id=${record.id}" }
                    // fnr (key) og message-innhold KUN til sikkerlogg (GDPR)
                    sikkerlogg.info { "Publiserte outbox id=${record.id} key=${record.key}" }
                } catch (e: Exception) {
                    logger.error(e) { "Feil ved publisering av outbox id=${record.id} — stopper polling" }
                    break
                }
            }
        }
    }

    private data class OutboxRecord(
        val id: Long,
        val key: String,
        val message: String,
    )
}
