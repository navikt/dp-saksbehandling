package no.nav.dagpenger.behandling.entitet

import java.time.LocalDate

internal class Periode(private val fomDato: LocalDate, private val tomDato: LocalDate) : ClosedRange<LocalDate> {
    override val endInclusive: LocalDate
        get() = tomDato
    override val start: LocalDate
        get() = fomDato

    init {
        require(start <= endInclusive) {
            "fomDato $start kan ikke være etter tomDato $endInclusive"
        }
    }

    companion object {
        internal infix fun LocalDate.til(fom: LocalDate) = Periode(this, fom)
    }
}
