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

    // Klageinstansen har gitt et utfall som krever at vi starter en ny revurdering av vedtaket
    // (Medhold, Delvis medhold, Opphevet, Retur eller Ugunst). kabalReferanse brukes til å knytte
    // den nye revurderingsbehandlingen til vedtaket i Kabal (KlageinstansVedtak.id).
    data class StartRevurdering(
        val klageBehandling: KlageBehandling,
        val kabalReferanse: UUID,
    ) : KlageAksjon() {
        override val behandlingId: UUID = klageBehandling.behandlingId
    }
}
