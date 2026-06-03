package no.nav.dagpenger.saksbehandling.utboks

/**
 * Tilstandene en utboks-melding kan ha. Eies av domenelogikken ([PostgresRapidUtboks]),
 * som bestemmer hvilken tilstand som lagres,
 * markeres og slettes. [UtboksRepository] kjenner ikke til disse verdiene — den
 * tar tilstand som [String] og er en ren persistens-seam.
 */
enum class UtboksTilstand {
    /** Nylig lagret, ikke publisert til Rapids & Rivers ennå. */
    PENDING,

    /** Publisert til Rapids & Rivers. Kan ryddes bort etter en periode. */
    SENDT,
}
