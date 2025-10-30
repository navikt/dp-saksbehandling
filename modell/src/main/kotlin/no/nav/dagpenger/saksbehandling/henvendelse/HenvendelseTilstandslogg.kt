package no.nav.dagpenger.saksbehandling.henvendelse

import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.hendelser.Hendelse
import no.nav.dagpenger.saksbehandling.henvendelse.Henvendelse.Tilstand
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

data class HenvendelseTilstandslogg(
    private val tilstandsendringer: MutableList<HenvendelseTilstandsendring> = mutableListOf(),
) : List<HenvendelseTilstandsendring> by tilstandsendringer {
    init {
        tilstandsendringer.sortertEtterTidspunkt()
    }

    fun leggTil(
        nyTilstand: Tilstand,
        hendelse: Hendelse,
    ) {
        tilstandsendringer.add(0, HenvendelseTilstandsendring(tilstand = nyTilstand, hendelse = hendelse))
    }

    private fun MutableList<HenvendelseTilstandsendring>.sortertEtterTidspunkt(): Unit = this.sortByDescending { it.tidspunkt }
}

data class HenvendelseTilstandsendring(
    val id: UUID = UUIDv7.ny(),
    val tilstand: Tilstand,
    val hendelse: Hendelse,
    val tidspunkt: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS),
)
