package no.nav.dagpenger.behandling

interface StegDeklerasjon {
    fun avhengerAv(id: String, svar: Svar<*> = Svar.Ubesvart, avhengerAv: AvhengerAv? = null): Steg
    fun avhengerAv(steg: Steg, avhengerAv: AvhengerAv? = null): Steg
}

typealias AvhengerAv = StegDeklerasjon.() -> Unit

class StegBuilder {
    private val steg = mutableSetOf<Steg>()
    fun getSteg() = steg
    fun steg(id: String, svar: Svar<*> = Svar.Ubesvart, avhengerAv: AvhengerAv? = null): Steg {
        val steg = Steg(id, svar).also {
            avhengerAv?.invoke(it)
        }
        this.steg.add(steg)
        return steg
    }

    fun steg(steg: Steg, avhengerAv: AvhengerAv? = null) {
        avhengerAv?.invoke(steg)
        this.steg.add(steg)
    }
}

fun steg(id: String, svar: Svar<*> = Svar.Ubesvart, avhengerAv: AvhengerAv? = null): Steg {
    return StegBuilder().steg(id, svar, avhengerAv)
}
