package no.nav.dagpenger.behandling.mengde

import kotlin.math.absoluteValue

// Forstår ulike mengder som kan måles som intervaller, som f.eks. temperatur
open class IntervallMengde internal constructor(mengde: Number, protected val enhet: Enhet) {
    protected val amount = mengde.toDouble()

    companion object {
        internal const val DELTA = 0.00000001
    }

    override fun equals(other: Any?) = other is IntervallMengde && this.equals(other)

    private fun equals(other: IntervallMengde) =
        this.isCompatible(other) &&
            (this.amount - convertedAmount(other)).absoluteValue < DELTA

    private fun isCompatible(other: IntervallMengde) = this.enhet.isCompatible(other.enhet)

    override fun hashCode() = enhet.hashCode(amount)

    protected fun convertedAmount(other: IntervallMengde) = this.enhet.convertedAmount(other.amount, other.enhet)
}
