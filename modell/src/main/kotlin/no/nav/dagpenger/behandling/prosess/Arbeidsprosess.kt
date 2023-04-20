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

    data class Overgang(val tilTilstand: Prosesstrinn, val guard: (() -> Boolean) = { true })

    fun leggTilTilstand(tilstand: Prosesstrinn, overgangs: List<Overgang>) {
        this.overganger[tilstand] = overgangs.toMutableList()
    }

    fun start(tilstand: Prosesstrinn) {
        gjeldendeTilstand = tilstand
        println("Starting work process at state $gjeldendeTilstand")
    }

    override fun gåTil(tilstand: Prosesstrinn) {
        if (gjeldendeTilstand == null) {
            throw IllegalStateException("Work process has not been started")
        }
        val currentTransitions =
            overganger[gjeldendeTilstand] ?: throw IllegalStateException("Invalid current state $gjeldendeTilstand")
        val transition = currentTransitions.firstOrNull { it.tilTilstand == tilstand && it.guard() }
            ?: throw IllegalStateException("Invalid transition from state $gjeldendeTilstand to state $tilstand")

        gjeldendeTilstand = tilstand
        println("Transitioning from state ${transition.tilTilstand} to state $gjeldendeTilstand at ${System.currentTimeMillis()}")
    }

    fun muligeTilstander(): List<Prosesstrinn> {
        if (gjeldendeTilstand == null) {
            throw IllegalStateException("Work process has not been started")
        }
        val currentTransitions =
            overganger[gjeldendeTilstand] ?: throw IllegalStateException("Invalid current state $gjeldendeTilstand")

        return currentTransitions.filter { it.guard() }.map { it.tilTilstand }
    }
}
