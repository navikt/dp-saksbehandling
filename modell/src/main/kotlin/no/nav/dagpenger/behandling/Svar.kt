package no.nav.dagpenger.behandling

class Svar<T>(val verdi: T?, val clazz: Class<T>, val sporing: Sporing) {
    fun besvar(verdi: T, sporing: Sporing) = Svar(verdi, this.clazz, sporing)
    fun nullstill() = Svar(null, this.clazz, NullSporing())
    val ubesvart get() = verdi == null

    override fun toString() = verdi.toString()
}
