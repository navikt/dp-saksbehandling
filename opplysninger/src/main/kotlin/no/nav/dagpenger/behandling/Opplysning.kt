package no.nav.dagpenger.behandling

import java.time.LocalDateTime

sealed class Opplysning<T : Comparable<T>>(
    val opplysningstype: Opplysningstype<T>,
    val verdi: T,
    val gyldighetsperiode: Gyldighetsperiode,
) : Klassifiserbart by opplysningstype {
    abstract fun bekreft(): Faktum<T>

    override fun toString() = "${javaClass.simpleName} om $opplysningstype har verdi: $verdi"

    fun sammeSom(opplysning: Opplysning<*>): Boolean {
        return opplysningstype == opplysning.opplysningstype && gyldighetsperiode.overlapp(opplysning.gyldighetsperiode)
    }
}

class Hypotese<T : Comparable<T>>(
    opplysningstype: Opplysningstype<T>,
    verdi: T,
    gyldighetsperiode: Gyldighetsperiode = Gyldighetsperiode(),
) : Opplysning<T>(opplysningstype, verdi, gyldighetsperiode) {
    constructor(opplysningstype: Opplysningstype<T>, verdi: T, gyldighetsperiode: ClosedRange<LocalDateTime>) : this(
        opplysningstype,
        verdi,
        Gyldighetsperiode(gyldighetsperiode.start, gyldighetsperiode.endInclusive),
    )

    override fun bekreft() = Faktum(super.opplysningstype, verdi, gyldighetsperiode)
}

class Faktum<T : Comparable<T>>(
    opplysningstype: Opplysningstype<T>,
    verdi: T,
    gyldighetsperiode: Gyldighetsperiode = Gyldighetsperiode(),
) : Opplysning<T>(opplysningstype, verdi, gyldighetsperiode) {
    constructor(opplysningstype: Opplysningstype<T>, verdi: T, gyldighetsperiode: ClosedRange<LocalDateTime>) : this(
        opplysningstype,
        verdi,
        Gyldighetsperiode(gyldighetsperiode.start, gyldighetsperiode.endInclusive),
    )

    override fun bekreft() = this
}
