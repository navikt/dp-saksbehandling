package no.nav.dagpenger.saksbehandling.statistikk.db

import no.nav.dagpenger.saksbehandling.statistikk.OppgaveITilstand
import java.util.UUID

interface SaksbehandlingsstatistikkRepository {
    fun tidligereTilstandsendringerErOverført(): Boolean

    fun oppgaveTilstandsendringer(): List<OppgaveITilstand>

    fun markerTilstandsendringerSomOverført(tilstandId: UUID)
}
