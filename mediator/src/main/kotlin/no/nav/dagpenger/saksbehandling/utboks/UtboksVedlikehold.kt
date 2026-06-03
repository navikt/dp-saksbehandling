package no.nav.dagpenger.saksbehandling.utboks

/**
 * Jobb-seam for utboks. Brukes av bakgrunnsjobbene ([UtboksPubliseringJob],
 * [UtboksOppryddingJob]) — ikke av mediatorene, som kun trenger [Utboks.send].
 *
 * Implementeres av [PostgresRapidUtboks], som eier all logikk.
 */
interface UtboksVedlikehold {
    /** Publiserer ventende (PENDING) meldinger til Rapids & Rivers og markerer dem som sendt. */
    fun publiserVentende()

    /** Rydder bort gamle, allerede sendte meldinger. */
    fun slettGamleSendte(): Int
}
