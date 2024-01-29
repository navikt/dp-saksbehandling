package no.nav.dagpenger.behandling

sealed class Opplysning<T : Comparable<T>>(
    val opplysningstype: Opplysningstype<T>,
    val verdi: T,
) : Klassifiserbart by opplysningstype {
    abstract fun bekreft(): Faktum<T>

    fun avhengerAv() = opplysningstype.bestårAv()
}

class Hypotese<T : Comparable<T>>(
    opplysningstype: Opplysningstype<T>,
    verdi: T,
) : Opplysning<T>(opplysningstype, verdi) {
    override fun bekreft() = Faktum<T>(super.opplysningstype, verdi)
}

class Faktum<T : Comparable<T>>(
    opplysningstype: Opplysningstype<T>,
    verdi: T,
) : Opplysning<T>(opplysningstype, verdi) {
    override fun bekreft() = this
}
