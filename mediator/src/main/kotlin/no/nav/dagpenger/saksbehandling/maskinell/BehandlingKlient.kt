package no.nav.dagpenger.saksbehandling.maskinell

import no.nav.dagpenger.behandling.opplysninger.api.models.BehandlingDTO
import java.util.UUID

interface BehandlingKlient {
    suspend fun hentBehandling(behandlingId: UUID, saksbehandlerToken: String): Pair<BehandlingDTO, Map<String, Any>>

    suspend fun godkjennBehandling(behandlingId: UUID, ident: String, saksbehandlerToken: String): Int
}
