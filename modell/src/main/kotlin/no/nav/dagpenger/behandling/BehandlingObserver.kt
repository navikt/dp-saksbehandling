package no.nav.dagpenger.behandling

import java.util.UUID

interface BehandlingObserver {
    fun behandlingEndretTilstand(søknadEndretTilstandEvent: BehandlingEndretTilstand) {}

    data class BehandlingEndretTilstand(
        val behandlingId: UUID,
        override val ident: String,
        val gjeldendeTilstand: String,
        val forrigeTilstand: String,
    ) : BehandlingEvent {
        override fun toMap() = mapOf(
            "behandlingId" to behandlingId,
            "ident" to ident,
            "gjeldendeTilstand" to gjeldendeTilstand,
            "forrigeTilstand" to forrigeTilstand,
        )
    }

    fun vedtakFattet(vedtakFattetEvent: VedtakFattet) {}

    data class VedtakFattet(
        val behandlingId: UUID,
        override val ident: String,
        val utfall: Boolean,
        val fastsettelser: Map<String, String>,
    ) : BehandlingEvent {
        override fun toMap() = mapOf(
            "behandlingId" to behandlingId,
            "ident" to ident,
            "innvilget" to utfall,
            "utfall" to utfall,
        ) + fastsettelser
    }

    interface BehandlingEvent {
        val ident: String
        fun toMap(): Map<String, Any>
    }
}
