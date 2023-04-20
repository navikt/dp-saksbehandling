package no.nav.dagpenger.behandling.prosess

typealias Prosesstrinn = String

class Arbeidsprosess {
    private val overganger = mutableMapOf<Prosesstrinn, MutableList<Overgang>>()
    private var gjeldendeTilstand: Prosesstrinn? = null

    fun currentState() = gjeldendeTilstand

    data class Overgang(val tilTilstand: Prosesstrinn, val guard: (() -> Boolean) = { true })

    fun leggTilTilstand(tilstand: Prosesstrinn, overgangs: List<Overgang>) {
        this.overganger[tilstand] = overgangs.toMutableList()
    }

    fun start(tilstand: Prosesstrinn) {
        gjeldendeTilstand = tilstand
        println("Starting work process at state $gjeldendeTilstand")
    }

    fun transitionTo(state: Prosesstrinn) {
        if (gjeldendeTilstand == null) {
            throw IllegalStateException("Work process has not been started")
        }
        val currentTransitions =
            overganger[gjeldendeTilstand] ?: throw IllegalStateException("Invalid current state $gjeldendeTilstand")
        val transition = currentTransitions.firstOrNull { it.tilTilstand == state && it.guard() }
            ?: throw IllegalStateException("Invalid transition from state $gjeldendeTilstand to state $state")

        gjeldendeTilstand = state
        println("Transitioning from state ${transition.tilTilstand} to state $gjeldendeTilstand at ${System.currentTimeMillis()}")
    }

    fun validTransitions(): List<Prosesstrinn> {
        if (gjeldendeTilstand == null) {
            throw IllegalStateException("Work process has not been started")
        }
        val currentTransitions =
            overganger[gjeldendeTilstand] ?: throw IllegalStateException("Invalid current state $gjeldendeTilstand")

        return currentTransitions.filter { it.guard() }.map { it.tilTilstand }
    }
}
