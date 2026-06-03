package no.nav.dagpenger.saksbehandling.utboks

import no.nav.dagpenger.saksbehandling.db.Transaksjonskontekst
import java.time.LocalDateTime

/**
 * Persistens-seam for utboks-tabellen. Samler all SQL-tilgang ett sted slik at
 * [PostgresRapidUtboks] holdes fri for inline SQL.
 *
 * Repositoryet er bevisst uvitende om hvilke tilstander som finnes — `tilstand`
 * tas som [String]. Domenelogikken (via [UtboksTilstand]) bestemmer hvilken
 * tilstand som lagres, hentes, oppdateres og slettes.
 */
interface UtboksRepository {
    /** Skriver en melding til utboks med [tilstand] i en pågående transaksjon (delt med domeneendringen). */
    fun lagre(
        key: String,
        message: String,
        tilstand: String,
        ctx: Transaksjonskontekst.Aktiv,
    )

    /**
     * Henter meldinger med [tilstand] (FIFO, begrenset til [limit]) og returnerer også totalt antall
     * meldinger med tilstanden — i én spørring via window-funksjon.
     *
     * Returnerer [Pair] der [Pair.first] er batch-listen og [Pair.second] er totalt antall (uavhengig av limit).
     */
    fun hentOgTellMedTilstand(
        tilstand: String,
        limit: Int = 100,
    ): Pair<List<UtboksMelding>, Int>

    /** Setter [tilstand] på meldingen med [id]. */
    fun oppdaterTilstand(
        id: Long,
        tilstand: String,
    )

    /** Sletter meldinger med [tilstand] eldre enn [cutoff]. Returnerer antall slettede rader. */
    fun slettMedTilstandEldreEnn(
        tilstand: String,
        cutoff: LocalDateTime,
    ): Int
}

data class UtboksMelding(
    val id: Long,
    val key: String,
    val message: String,
    val tilstand: String,
)
