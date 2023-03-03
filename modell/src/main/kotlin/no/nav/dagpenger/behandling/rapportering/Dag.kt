package no.nav.dagpenger.behandling.rapportering

import no.nav.dagpenger.behandling.entitet.Arbeidstimer
import no.nav.dagpenger.behandling.entitet.Arbeidstimer.Companion.arbeidstimer
import no.nav.dagpenger.behandling.entitet.Periode
import no.nav.dagpenger.behandling.visitor.DagVisitor
import java.time.DayOfWeek
import java.time.LocalDate

sealed class Dag(private val dato: LocalDate) {
    abstract fun accept(visitor: DagVisitor)
    abstract fun arbeidstimer(): Arbeidstimer

    internal fun innenfor(periode: Periode) = dato in periode

    companion object {
        internal fun fraværsdag(dato: LocalDate) = Fraværsdag(dato)

        internal fun arbeidsdag(dato: LocalDate, arbeidstimer: Arbeidstimer): Dag {
            return if (dato.erHelg()) {
                Helgedag(dato, arbeidstimer)
            } else {
                Arbeidsdag(dato, arbeidstimer)
            }
        }

        private fun LocalDate.erHelg() = dayOfWeek in setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
    }
}

class Fraværsdag(dato: LocalDate) : Dag(dato) {
    override fun arbeidstimer(): Arbeidstimer = 0.arbeidstimer
    override fun accept(visitor: DagVisitor) {
        visitor.visitFraværsdag(this)
    }
}

class Arbeidsdag(dato: LocalDate, private val arbeidstimer: Arbeidstimer) : Dag(dato) {
    override fun arbeidstimer(): Arbeidstimer = arbeidstimer
    override fun accept(visitor: DagVisitor) {
        visitor.visitArbeidsdag(this)
    }
}
class Helgedag(dato: LocalDate, private val arbeidstimer: Arbeidstimer) : Dag(dato) {
    override fun arbeidstimer(): Arbeidstimer = arbeidstimer
    override fun accept(visitor: DagVisitor) {
        visitor.visitHelgedag(this)
    }
}
