package no.nav.dagpenger.saksbehandling.statistikk.db

import no.nav.dagpenger.saksbehandling.statistikk.OppgaveITilstand
import java.util.UUID

interface SaksbehandlingsstatistikkRepository {
    companion object {
        /**
         * Batchstørrelse for begge stegene i eksporten: hvor mange kandidater som materialiseres
         * per kjøring, og hvor mange uleverte rader jobben henter. De må være samme tall — jobben
         * materialiserer ikke nytt før backloggen er tom, og en backlog kan aldri bli større enn
         * det materialiseringen selv produserte.
         */
        const val ANTALL_PER_KJØRING = 1000
    }

    /**
     * Rader som er materialisert, men ikke bekreftet levert til statistikk.
     *
     * Sortert på sekvensnummer, som er materialiseringsrekkefølgen. Uten sortering ville en retry
     * kunne levere tilstandsendringer for samme person i vilkårlig rekkefølge.
     */
    fun oppgaveTilstandsendringerIkkeOverfort(antall: Int): List<OppgaveITilstand>

    /**
     * Tar de neste kandidatene fra `statistikk_kandidat_v1`, beriker dem og legger dem i
     * `saksbehandling_statistikk_v1` klare til publisering.
     *
     * Returnerer bare kandidatene som faktisk kvalifiserte. Kandidater som filtreres bort
     * (Oppfølging, rader under gulvene) markeres også som vurdert, ellers ville de blitt liggende
     * for alltid.
     *
     * Uttaket og markeringen skjer i én transaksjon, slik at en kandidat ikke kan bli markert som
     * vurdert uten å bli materialisert.
     */
    fun oppgaveTilstandsendringer(): List<OppgaveITilstand>

    fun markerTilstandsendringerSomOverført(tilstandId: UUID): Int
}
