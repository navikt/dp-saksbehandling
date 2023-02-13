package no.nav.dagpenger.behandling.rapportering

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
}
