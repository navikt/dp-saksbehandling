package no.nav.dagpenger.saksbehandling.outbox

/**
 * Tilstandene en outbox-record kan ha. Eies av domenelogikken ([PostgresRapidOutbox]),
 * som bestemmer hvilken tilstand som lagres,
 * markeres og slettes. [OutboxRepository] kjenner ikke til disse verdiene — den
 * tar tilstand som [String] og er en ren persistens-seam.
 */
enum class OutboxTilstand {
    /** Nylig lagret, ikke publisert til Rapids & Rivers ennå. */
    PENDING,

    /** Publisert til Rapids & Rivers. Kan ryddes bort etter en periode. */
    SENDT,
}
