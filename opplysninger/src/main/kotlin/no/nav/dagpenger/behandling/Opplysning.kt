package no.nav.dagpenger.behandling

sealed class Opplysning(protected val opplysningstype: Opplysningstype) : Klassifiserbart by opplysningstype {
    abstract fun bekreft(): Faktum

    fun avhengerAv() = opplysningstype.bestårAv()
}

class Etterlyst(opplysningstype: Opplysningstype) : Opplysning(opplysningstype) {
    override fun bekreft() = Faktum(super.opplysningstype)
}

class Hypotese(opplysningstype: Opplysningstype) : Opplysning(opplysningstype) {
    override fun bekreft() = Faktum(super.opplysningstype)
}

class Faktum(opplysningstype: Opplysningstype) : Opplysning(opplysningstype) {
    override fun bekreft() = this
}
