package no.nav.dagpenger.saksbehandling.statistikk.db

import no.nav.dagpenger.saksbehandling.statistikk.OppgaveITilstand
import java.util.UUID

interface SaksbehandlingsstatistikkRepository {
    fun oppgaveTilstandsendringerIkkeOverfort(): List<OppgaveITilstand>

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
