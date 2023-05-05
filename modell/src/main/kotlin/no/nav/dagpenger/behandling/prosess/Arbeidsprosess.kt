package no.nav.dagpenger.behandling.prosess

typealias Prosesstrinn = String

interface IArbeidsprosess {
    fun tilstand(): Prosesstrinn?
    fun gåTil(tilstand: Prosesstrinn)
}

class Arbeidsprosess : IArbeidsprosess {
    private val overganger = mutableMapOf<Prosesstrinn, MutableList<Overgang>>()
    private var gjeldendeTilstand: Prosesstrinn? = null

    override fun tilstand() = gjeldendeTilstand

    data class Overgang(
        val tilTilstand: Prosesstrinn,
        val vedOvergang: (() -> Unit) = {},
        val guard: (() -> Boolean) = { true },
    )

    fun leggTilTilstand(tilstand: Prosesstrinn) = leggTilTilstand(tilstand, emptyList())

    fun leggTilTilstand(tilstand: Prosesstrinn, muligeOverganger: List<Overgang>) {
        this.overganger[tilstand] = muligeOverganger.toMutableList()
    }

    fun leggTilTilstand(tilstand: Prosesstrinn, vararg muligeOverganger: Overgang) =
        leggTilTilstand(tilstand, muligeOverganger.toList())

    fun start(tilstand: Prosesstrinn) {
        gjeldendeTilstand = tilstand
        println("Starting work process at state $gjeldendeTilstand")
    }

    override fun gåTil(tilstand: Prosesstrinn) {
        if (gjeldendeTilstand == null) {
            throw IllegalStateException("Work process has not been started")
        }
        val muligeOverganger =
            overganger[gjeldendeTilstand] ?: throw IllegalStateException("Invalid current state $gjeldendeTilstand")

        val overgang = muligeOverganger.firstOrNull { it.tilTilstand == tilstand && it.guard() }
            ?: throw IllegalStateException("Invalid transition from state $gjeldendeTilstand to state $tilstand")

        val forrigeTilstand = gjeldendeTilstand

        gjeldendeTilstand = tilstand.also {
            overgang.vedOvergang()
        }

        println("Transitioning from state $forrigeTilstand to state $tilstand at ${System.currentTimeMillis()}")
    }

    fun muligeTilstander(): List<Prosesstrinn> {
        if (gjeldendeTilstand == null) {
            throw IllegalStateException("Work process has not been started")
        }
        val muligeOverganger =
            overganger[gjeldendeTilstand] ?: throw IllegalStateException("Invalid current state $gjeldendeTilstand")

        return muligeOverganger.filter { it.guard() }.map { it.tilTilstand }
    }
}
