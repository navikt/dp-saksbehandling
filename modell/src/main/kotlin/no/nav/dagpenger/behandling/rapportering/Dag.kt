package no.nav.dagpenger.behandling.rapportering

import java.time.DayOfWeek
import java.time.LocalDate

internal sealed class Dag(dato: LocalDate) {
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

internal class Fraværsdag(dato: LocalDate) : Dag(dato)
internal class Arbeidsdag(dato: LocalDate) : Dag(dato)
internal class Helgedag(dato: LocalDate) : Dag(dato)
