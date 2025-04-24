package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.hendelser.Hendelse
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

data class Tilstandslogg(
    private val tilstandsendringer: MutableList<Tilstandsendring> = mutableListOf(),
) : List<Tilstandsendring> by tilstandsendringer {
    constructor(
        vararg tilstandsendringer: Tilstandsendring,
    ) : this() {
        this.tilstandsendringer.addAll(tilstandsendringer.toList())
    }

    companion object {
        fun rehydrer(tilstandsendringer: List<Tilstandsendring>): Tilstandslogg = Tilstandslogg(tilstandsendringer.toMutableList())
    }

    init {
        tilstandsendringer.sorterEtterTidspunkt()
    }

    fun leggTil(
        nyTilstand: Oppgave.Tilstand.Type,
        hendelse: Hendelse,
    ) {
        tilstandsendringer.add(0, Tilstandsendring(tilstand = nyTilstand, hendelse = hendelse))
    }

    private fun MutableList<Tilstandsendring>.sorterEtterTidspunkt(): Unit = this.sortByDescending { it.tidspunkt }
}

data class Tilstandsendring(
    val id: UUID = UUIDv7.ny(),
    val tilstand: Oppgave.Tilstand.Type,
    val hendelse: Hendelse,
    val tidspunkt: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS),
)
