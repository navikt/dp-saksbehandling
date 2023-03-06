package no.nav.dagpenger.behandling.entitet

class Timer(timer: Number) : Comparable<Timer> {
    private val timer = timer.toDouble()

    init {
        require(this.timer >= 0) { "Timer må være større enn eller lik 0, er ${this.timer}" }
    }

    internal operator fun div(nevner: Timer): Prosent {
        if (nevner.timer == 0.0) return Prosent(0)
        return Prosent(this.timer / nevner.timer * 100)
    }

    operator fun times(dager: Int): Timer = Timer(this.timer * dager)
    override fun compareTo(other: Timer): Int = this.timer.compareTo(other.timer)

    override fun equals(other: Any?) = other is Timer && other.timer == this.timer

    override fun hashCode(): Int = timer.hashCode()

    override fun toString() = "Timer($timer)"

    companion object {
        val Number.timer get() = Timer(this)
        fun List<Timer>.summer() = this.sumOf { it.timer }.timer
    }
}
