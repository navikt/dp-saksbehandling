package no.nav.dagpenger.behandling.rapportering

import no.nav.dagpenger.behandling.visitor.DagVisitor
import java.time.DayOfWeek
import java.time.LocalDate

sealed class Dag(private val dato: LocalDate) {
    abstract fun accept(visitor: DagVisitor)

    internal fun innenfor(periode: Periode) = dato in periode

    companion object {
        fun fraværsdag(dato: LocalDate) = Fraværsdag(dato)

        fun arbeidsdag(dato: LocalDate): Dag {
            return if (dato.dayOfWeek in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)) {
                Helgedag(dato)
            } else {
                Arbeidsdag(dato)
            }
        }
    }
}

class Fraværsdag(dato: LocalDate) : Dag(dato) {
    override fun accept(visitor: DagVisitor) {
        visitor.visitFraværsdag(this)
    }
}

class Arbeidsdag(dato: LocalDate) : Dag(dato) {
    override fun accept(visitor: DagVisitor) {
        visitor.visitArbeidsdag(this)
    }
}
class Helgedag(dato: LocalDate) : Dag(dato) {
    override fun accept(visitor: DagVisitor) {
        visitor.visitHelgedag(this)
    }
}
