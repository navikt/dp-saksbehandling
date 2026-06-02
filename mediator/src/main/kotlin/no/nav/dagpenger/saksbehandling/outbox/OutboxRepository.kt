package no.nav.dagpenger.saksbehandling.outbox

import no.nav.dagpenger.saksbehandling.db.Transaksjonskontekst
import java.time.LocalDateTime

/**
 * Persistens-seam for outbox-tabellen. Samler all SQL-tilgang ett sted slik at
 * [PostgresOutbox], [OutboxPublisher] og [OutboxCleanupJob] holdes fri for inline SQL.
 *
 * Repositoryet er bevisst uvitende om hvilke tilstander som finnes — `tilstand`
 * tas som [String]. Domenelogikken (via [OutboxTilstand]) bestemmer hvilken
 * tilstand som lagres, hentes, oppdateres og slettes.
 */
interface OutboxRepository {
    /** Skriver en melding til outbox med [tilstand] i en pågående transaksjon (delt med domeneendringen). */
    fun lagre(
        key: String,
        message: String,
        tilstand: String,
        ctx: Transaksjonskontekst.Aktiv,
    )

    /** Henter records med [tilstand] i global FIFO-rekkefølge (ORDER BY id), begrenset til [limit]. */
    fun hentMedTilstand(
        tilstand: String,
        limit: Int = 100,
    ): List<OutboxRecord>

    /** Setter [tilstand] på recorden med [id]. */
    fun oppdaterTilstand(
        id: Long,
        tilstand: String,
    )

    /** Sletter records med [tilstand] eldre enn [cutoff]. Returnerer antall slettede rader. */
    fun slettMedTilstandEldreEnn(
        tilstand: String,
        cutoff: LocalDateTime,
    ): Int
}

data class OutboxRecord(
    val id: Long,
    val key: String,
    val message: String,
    val tilstand: String,
)
