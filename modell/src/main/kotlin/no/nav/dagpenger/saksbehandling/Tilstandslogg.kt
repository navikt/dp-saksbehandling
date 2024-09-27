package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.hendelser.Hendelse
import java.time.LocalDateTime

data class Tilstandslogg(
    private val tilstandsendringer: MutableList<Tilstandsendring> = mutableListOf(),
) : List<Tilstandsendring> by tilstandsendringer {
    fun leggTil(
        nyTilstand: Oppgave.Tilstand,
        hendelse: Hendelse,
    ) {
        tilstandsendringer.add(0, Tilstandsendring(nyTilstand.type, hendelse))
    }
}

data class Tilstandsendring(
    val tilstand: Oppgave.Tilstand.Type,
    val hendelse: Hendelse,
    val tidspunkt: LocalDateTime = LocalDateTime.now(),
)
