package no.nav.dagpenger.saksbehandling.klage

import java.util.UUID

sealed class KlageAksjon {
    abstract val behandlingId: UUID

    data class IngenAksjon(
        override val behandlingId: UUID,
    ) : KlageAksjon()

    data class OversendKlageinstans(
        val klageBehandling: KlageBehandling,
    ) : KlageAksjon() {
        override val behandlingId: UUID = klageBehandling.behandlingId
    }
}
