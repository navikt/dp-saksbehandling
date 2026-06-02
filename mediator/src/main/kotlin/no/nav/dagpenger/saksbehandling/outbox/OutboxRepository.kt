package no.nav.dagpenger.saksbehandling.outbox

import no.nav.dagpenger.saksbehandling.db.Transaksjonskontekst
import java.time.LocalDateTime

/**
 * Persistens-seam for outbox-tabellen. Samler all SQL-tilgang ett sted slik at
 * [PostgresOutbox], [OutboxPublisher] og [OutboxCleanupJob] holdes fri for inline SQL.
 */
interface OutboxRepository {
    /** Skriver en melding til outbox i en pågående transaksjon (delt med domeneendringen). */
    fun lagre(
        key: String,
        message: String,
        ctx: Transaksjonskontekst.Aktiv,
    )

    /** Henter PENDING-records i global FIFO-rekkefølge (ORDER BY id), begrenset til [limit]. */
    fun hentPending(limit: Int = 100): List<OutboxRecord>

    /** Markerer en record som SENDT etter vellykket publisering. */
    fun markerSendt(id: Long)

    /** Sletter SENDT-records eldre enn [cutoff]. Returnerer antall slettede rader. */
    fun slettSendteEldreEnn(cutoff: LocalDateTime): Int
}

data class OutboxRecord(
    val id: Long,
    val key: String,
    val message: String,
)
