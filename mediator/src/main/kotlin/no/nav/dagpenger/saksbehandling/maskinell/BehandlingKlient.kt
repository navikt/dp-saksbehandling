package no.nav.dagpenger.saksbehandling.maskinell

import java.util.UUID

interface BehandlingKlient {
    suspend fun hentBehandling(behandlingId: UUID, saksbehandlerToken: String): Map<String, Any>

    suspend fun godkjennBehandling(behandlingId: UUID, ident: String, saksbehandlerToken: String): Int
    suspend fun avbrytBehandling(behandlingId: UUID, ident: String, saksbehandlerToken: String): Int
}
