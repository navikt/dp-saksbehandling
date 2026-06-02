package no.nav.dagpenger.saksbehandling.outbox

/**
 * Jobb-seam for outbox. Brukes av bakgrunnsjobbene ([OutboxPublisherJob],
 * [OutboxCleanupJob]) — ikke av mediatorene, som kun trenger [Outbox.send].
 *
 * Implementeres av [OutboxTjeneste], som eier all logikk.
 */
interface OutboxVedlikehold {
    /** Publiserer ventende (PENDING) meldinger til Rapids & Rivers og markerer dem som sendt. */
    fun publiserVentende()

    /** Rydder bort gamle, allerede sendte meldinger. */
    fun slettGamleSendte()
}
