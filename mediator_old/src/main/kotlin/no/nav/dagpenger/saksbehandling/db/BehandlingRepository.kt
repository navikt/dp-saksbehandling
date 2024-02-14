package no.nav.dagpenger.saksbehandling.db

import no.nav.dagpenger.saksbehandling.Behandling
import java.util.UUID

interface BehandlingRepository {
    fun hentBehandling(uuid: UUID): Behandling
}
