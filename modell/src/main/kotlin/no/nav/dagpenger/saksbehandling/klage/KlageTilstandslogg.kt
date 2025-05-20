package no.nav.dagpenger.saksbehandling.klage

import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.hendelser.Hendelse
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

data class KlageTilstandslogg(
    private val tilstandsendringer: MutableList<KlageTilstandsendring> = mutableListOf(),
) : List<KlageTilstandsendring> by tilstandsendringer {
    constructor(
        vararg tilstandsendringer: KlageTilstandsendring,
    ) : this() {
        this.tilstandsendringer.addAll(tilstandsendringer.toList())
    }

    companion object {
        fun rehydrer(tilstandsendringer: List<KlageTilstandsendring>): KlageTilstandslogg =
            KlageTilstandslogg(tilstandsendringer.toMutableList())
    }

    init {
        tilstandsendringer.sorterEtterTidspunkt()
    }

    fun leggTil(
        nyTilstand: KlageBehandling.KlageTilstand.Type,
        hendelse: Hendelse,
    ) {
        tilstandsendringer.add(0, KlageTilstandsendring(tilstand = nyTilstand, hendelse = hendelse))
    }

    private fun MutableList<KlageTilstandsendring>.sorterEtterTidspunkt(): Unit = this.sortByDescending { it.tidspunkt }
}

data class KlageTilstandsendring(
    val id: UUID = UUIDv7.ny(),
    val tilstand: KlageBehandling.KlageTilstand.Type,
    val hendelse: Hendelse,
    val tidspunkt: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS),
)
