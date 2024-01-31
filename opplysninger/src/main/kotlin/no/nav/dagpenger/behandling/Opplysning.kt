package no.nav.dagpenger.behandling

import java.time.LocalDate
import java.time.LocalDateTime

data class Gyldighetsperiode(val fom: LocalDateTime = LocalDateTime.MIN, private val tom: LocalDateTime = LocalDateTime.MAX) {
    constructor(fom: LocalDate, tom: LocalDate) : this(fom.atStartOfDay(), tom.atStartOfDay())

    fun inneholder(dato: LocalDateTime) = dato in fom..tom

    fun inneholder(dato: LocalDate) = inneholder(dato.atStartOfDay())
}

sealed class Opplysning<T : Comparable<T>>(
    val opplysningstype: Opplysningstype<T>,
    val verdi: T,
    val gyldighetsperiode: Gyldighetsperiode,
) : Klassifiserbart by opplysningstype {
    abstract fun bekreft(): Faktum<T>

    override fun toString() = "${javaClass.simpleName} om $opplysningstype har verdi: $verdi"
}

class Hypotese<T : Comparable<T>>(
    opplysningstype: Opplysningstype<T>,
    verdi: T,
    gyldighetsperiode: Gyldighetsperiode = Gyldighetsperiode(),
) : Opplysning<T>(opplysningstype, verdi, gyldighetsperiode) {
    override fun bekreft() = Faktum(super.opplysningstype, verdi, gyldighetsperiode)
}

class Faktum<T : Comparable<T>>(
    opplysningstype: Opplysningstype<T>,
    verdi: T,
    gyldighetsperiode: Gyldighetsperiode = Gyldighetsperiode(),
) : Opplysning<T>(opplysningstype, verdi, gyldighetsperiode) {
    override fun bekreft() = this
}
