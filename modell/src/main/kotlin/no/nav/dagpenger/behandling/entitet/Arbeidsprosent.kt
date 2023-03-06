package no.nav.dagpenger.behandling.entitet

internal class Arbeidsprosent(prosent: Number) : Comparable<Arbeidsprosent> {
    private val prosent = prosent.toDouble()

    init {
        require(this.prosent >= 0) { "Arbeidsprosent må være større enn eller lik 0, er ${this.prosent}" }
    }

    override fun compareTo(other: Arbeidsprosent): Int = this.prosent.compareTo(other.prosent)

    override fun equals(other: Any?): Boolean = other is Arbeidsprosent && other.prosent == this.prosent

    override fun hashCode(): Int = prosent.hashCode()
    override fun toString(): String {
        return "Arbeidsprosent(prosent=$prosent)"
    }
}
