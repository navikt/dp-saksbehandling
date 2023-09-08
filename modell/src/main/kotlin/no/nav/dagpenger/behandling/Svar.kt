package no.nav.dagpenger.behandling

import java.time.LocalDateTime

class Svar<T>(val verdi: T?, val clazz: Class<T>, val sporing: Sporing) {
    fun besvar(verdi: T, sporing: Sporing) = Svar(verdi, this.clazz, sporing)
    fun nullstill() = Svar(null, this.clazz, NullSporing(LocalDateTime.now()))
    val ubesvart get() = verdi == null

    override fun toString() = verdi.toString()
}
