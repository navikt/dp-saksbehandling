package no.nav.dagpenger.saksbehandling.statistikk.db

import no.nav.dagpenger.saksbehandling.statistikk.OppgaveITilstand
import java.util.UUID

interface SaksbehandlingsstatistikkRepository {
    companion object {
        /**
         * Batchstørrelse for begge stegene i eksporten: hvor mange kandidater som skrives til
         * statistikk per kjøring, og hvor mange uleverte rader jobben henter.
         */
        const val ANTALL_PER_KJØRING = 1000
    }

    fun oppgaveTilstandsendringerIkkeOverfort(antall: Int): List<OppgaveITilstand>

    /**
     * Tar de neste kandidatene fra `statistikk_kandidat_v1`, beriker dem og legger dem i
     * `saksbehandling_statistikk_v1` klare til publisering.
     */
    fun oppgaveTilstandsendringer(): List<OppgaveITilstand>

    fun markerTilstandsendringerSomOverført(tilstandId: UUID): Int
}
