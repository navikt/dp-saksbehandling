package no.nav.dagpenger.behandling.db

import no.nav.dagpenger.behandling.Behandling
import java.util.UUID

interface BehandlingRepository {
    fun hentBehandling(uuid: UUID): Behandling
}
