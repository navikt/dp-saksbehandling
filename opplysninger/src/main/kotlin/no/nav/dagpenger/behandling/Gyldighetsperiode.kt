package no.nav.dagpenger.behandling

import java.time.LocalDate
import java.time.LocalDateTime

data class Gyldighetsperiode(
    val fom: LocalDateTime = LocalDateTime.MIN,
    private val tom: LocalDateTime = LocalDateTime.MAX,
    private val range: ClosedRange<LocalDateTime> = fom..tom,
) : ClosedRange<LocalDateTime> by range {
    constructor(fom: LocalDate, tom: LocalDate) : this(fom.atStartOfDay(), tom.atStartOfDay())
    constructor(fom: LocalDate) : this(fom.atStartOfDay(), LocalDateTime.now())

    fun inneholder(dato: LocalDateTime) = dato in range

    fun inneholder(dato: LocalDate) = inneholder(dato.atStartOfDay())

    fun overlapp(gyldighetsperiode: Gyldighetsperiode) =
        this.contains(gyldighetsperiode.fom) || this.contains(gyldighetsperiode.fom) ||
            gyldighetsperiode.contains(this.fom) || gyldighetsperiode.contains(this.fom)
}
