package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.hendelser.Hendelse
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class OppgaveTilstandslogg(
    tilstandsendringer: List<Tilstandsendring<Oppgave.Tilstand.Type>> = listOf(),
) : Tilstandslogg<Oppgave.Tilstand.Type>(tilstandsendringer.toMutableList()) {
    constructor(vararg tilstandsEndringer: Tilstandsendring<Oppgave.Tilstand.Type>) : this(tilstandsEndringer.toMutableList())
}

open class Tilstandslogg<T : Enum<T>>(
    protected val tilstandsendringer: MutableList<Tilstandsendring<T>> = mutableListOf(),
) : List<Tilstandsendring<T>> by tilstandsendringer {
    constructor(vararg tilstandsendringer: Tilstandsendring<T>) : this(tilstandsendringer.toMutableList())

    init {
        tilstandsendringer.sorterEtterTidspunkt()
    }

    fun leggTil(
        nyTilstand: T,
        hendelse: Hendelse,
    ) {
        tilstandsendringer.add(0, Tilstandsendring<T>(tilstand = nyTilstand, hendelse = hendelse))
    }

    private fun MutableList<Tilstandsendring<T>>.sorterEtterTidspunkt(): Unit = this.sortByDescending { it.tidspunkt }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Tilstandslogg<*>

        return tilstandsendringer == other.tilstandsendringer
    }

    override fun hashCode(): Int {
        return tilstandsendringer.hashCode()
    }
}

data class Tilstandsendring<T : Enum<T>>(
    val id: UUID = UUIDv7.ny(),
    val tilstand: T,
    val hendelse: Hendelse,
    val tidspunkt: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS),
)
