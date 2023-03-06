package no.nav.dagpenger.behandling.entitet

internal class Prosent(prosent: Number) : Comparable<Prosent> {
    private val prosent = prosent.toDouble()

    init {
        require(this.prosent >= 0) { "Arbeidsprosent må være større enn eller lik 0, er ${this.prosent}" }
    }

    override fun compareTo(other: Prosent): Int = this.prosent.compareTo(other.prosent)

    override fun equals(other: Any?): Boolean = other is Prosent && other.prosent == this.prosent

    override fun hashCode(): Int = prosent.hashCode()
    override fun toString(): String {
        return "Prosent($prosent)"
    }
}
