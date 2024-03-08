package no.nav.dagpenger.saksbehandling.maskinell

import no.nav.dagpenger.behandling.opplysninger.api.models.BehandlingDTO
import java.util.UUID

interface BehandlingKlient {
    suspend fun hentBehandling(behandlingId: UUID, saksbehandlerToken: String): BehandlingDTO

    suspend fun bekreftBehandling(behandlingId: UUID, saksbehandlerToken: String)
}
