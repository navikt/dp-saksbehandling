@file:Suppress("SpellCheckingInspection")

package no.nav.dagpenger.behandling

interface StegDeklerasjon {
    fun avhengerAvFastsettelse(id: String, svar: Svar<*> = Svar.Ubesvart, avhengerAv: AvhengerAv? = null): Steg

    @Suppress("FunctionName")
    fun avhengerAvVilkår(id: String, svar: Svar<*> = Svar.Ubesvart, avhengerAv: AvhengerAv? = null): Steg
    fun avhengerAv(steg: Steg, avhengerAv: AvhengerAv? = null): Steg
}

typealias AvhengerAv = StegDeklerasjon.() -> Unit

class BehandlingBuilder {
    private val steg = mutableSetOf<Steg>()
    fun steg(block: StegBuilder.() -> Steg): Steg {
        return StegBuilder.block().also {
            this.steg.add(it)
        }
    }

    fun steg(steg: Steg, avhengerAv: AvhengerAv? = null): Steg {
        avhengerAv?.invoke(steg)
        this.steg.add(steg)
        return steg
    }

    fun getSteg(): Set<Steg> = steg
}

fun behandling(block: BehandlingBuilder.() -> Unit): Behandling {
    val builder = BehandlingBuilder()
    builder.block()
    return Behandling(builder.getSteg())
}
object StegBuilder {
    @Suppress("FunctionName")
    fun vilkår(id: String, svar: Svar<*> = Svar.Ubesvart, avhengerAv: AvhengerAv? = null): Steg.Vilkår {
        val steg = Steg.Vilkår(id, svar).also {
            avhengerAv?.invoke(it)
        }
        return steg
    }

    @Suppress("FunctionName")
    fun fastsettelse(id: String, svar: Svar<*> = Svar.Ubesvart, avhengerAv: AvhengerAv? = null): Steg.FastSettelse {
        val steg = Steg.FastSettelse(id, svar).also {
            avhengerAv?.invoke(it)
        }
        return steg
    }
}
