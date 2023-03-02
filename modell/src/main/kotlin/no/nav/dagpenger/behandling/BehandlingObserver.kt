package no.nav.dagpenger.behandling

import java.util.UUID

interface BehandlingObserver {
    fun behandlingTilstandEndret(event: BehandlingEndretTilstandEvent) {}

    data class BehandlingEndretTilstandEvent(
        val behandlingsId: UUID,
        val ident: String,
        val gjeldendeTilstand: Behandling.Tilstand.Type,
        val forrigeTilstand: Behandling.Tilstand.Type,
    )
}
