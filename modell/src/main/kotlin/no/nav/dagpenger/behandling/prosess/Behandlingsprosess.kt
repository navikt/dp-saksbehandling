package no.nav.dagpenger.behandling.prosess

import no.nav.dagpenger.behandling.Behandling

sealed class Behandlingsprosess(
    private val behandling: Behandling,
    private var tilstand: Prosesstilstand,
) {
    fun neste() = tilstand.neste?.takeIf { harNeste() }?.let {
        tilstand(it)
    } ?: throw IllegalStateException("Kan ikke endre tilstand")

    fun tilbake() = tilstand.tilbake?.takeIf { harTilbake() }?.let {
        tilstand(it)
    } ?: throw IllegalStateException("Kan ikke endre tilstand")

    fun harNeste() = tilstand.kanGåNeste(behandling)
    fun harTilbake() = tilstand.kanGåTilbake(behandling)

    private fun tilstand(nyTilstand: Prosesstilstand) {
        this.tilstand = nyTilstand
    }

    interface Prosesstilstand {
        val neste: Prosesstilstand? get() = null
        val tilbake: Prosesstilstand? get() = null

        fun kanGåNeste(behandling: Behandling) = neste != null
        fun kanGåTilbake(behandling: Behandling) = tilbake != null
    }
}

class Enkelprosess(
    behandling: Behandling,
) : Behandlingsprosess(behandling, TilBehandling) {
    private object TilBehandling : Prosesstilstand {
        override val neste = Avsluttet
    }

    private object Avsluttet : Prosesstilstand
}

class Totrinnsprosess(
    behandling: Behandling,
) : Behandlingsprosess(behandling, TilBehandling) {
    private object TilBehandling : Prosesstilstand {
        override val neste = Innstilt

        override fun kanGåNeste(behandling: Behandling) =
            behandling.harUtfall() != null
    }

    private object Innstilt : Prosesstilstand {
        override val neste = FerdigBehandlet
        override val tilbake = Innstilt
    }

    private object FerdigBehandlet : Prosesstilstand
}
