package no.nav.dagpenger.behandling.entitet

class Arbeidstimer(arbeidstimer: Number) : Comparable<Arbeidstimer> {
    private val arbeidstimer = arbeidstimer.toDouble()

    init {
        require(this.arbeidstimer >= 0) { "Arbeidstimer må være større enn eller lik 0, er ${this.arbeidstimer}" }
    }

    internal operator fun div(nevner: Arbeidstimer): Arbeidsprosent {
        if (nevner.arbeidstimer == 0.0) return Arbeidsprosent(0)
        return Arbeidsprosent(this.arbeidstimer / nevner.arbeidstimer * 100)
    }

    override fun compareTo(other: Arbeidstimer): Int = this.arbeidstimer.compareTo(other.arbeidstimer)

    override fun equals(other: Any?) = other is Arbeidstimer && other.arbeidstimer == this.arbeidstimer

    override fun hashCode(): Int = arbeidstimer.hashCode()

    override fun toString() = "Arbeidstimer(arbeidstimer=$arbeidstimer)"
    operator fun times(antallArbeidsdager: Int): Arbeidstimer = Arbeidstimer(this.arbeidstimer * antallArbeidsdager)

    companion object {
        val Number.arbeidstimer get() = Arbeidstimer(this)
        fun List<Arbeidstimer>.summer() = this.sumOf { it.arbeidstimer }.arbeidstimer
    }
}
