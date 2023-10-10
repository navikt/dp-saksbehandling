package no.nav.dagpenger.behandling

import java.util.UUID

interface BehandlingObserver {
    fun behandlingEndretTilstand(behandlingEndretTilstandEvent: BehandlingEndretTilstand) {}

    data class BehandlingEndretTilstand(
        val behandlingId: UUID,
        override val ident: String,
        val gjeldendeTilstand: String,
        val forrigeTilstand: String,
    ) : BehandlingEvent {
        override fun toMap() =
            mapOf(
                "behandlingId" to behandlingId,
                "ident" to ident,
                "gjeldendeTilstand" to gjeldendeTilstand,
                "forrigeTilstand" to forrigeTilstand,
            )
    }

    fun vedtakFattet(
        vedtakFattetEvent: VedtakFattet,
        kommando: UtførStegKommando,
    ) {}

    data class VedtakFattet(
        val behandlingId: UUID,
        override val ident: String,
        val utfall: Utfall,
        val fastsettelser: Map<String, String>,
        val sakId: UUID,
    ) : BehandlingEvent {
        override fun toMap() =
            mapOf(
                "behandlingId" to behandlingId,
                "ident" to ident,
                "utfall" to utfall.toString(),
                "sakId" to sakId,
            ) + fastsettelser
    }

    interface BehandlingEvent {
        val ident: String

        fun toMap(): Map<String, Any>
    }
}
