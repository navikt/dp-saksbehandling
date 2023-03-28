@file:Suppress("SpellCheckingInspection")

package no.nav.dagpenger.behandling

fun behandling(person: Person, block: BehandlingDSL.() -> Unit): Behandling {
    val dsl = BehandlingDSL()
    block(dsl)
    return Behandling(person, dsl.steg)
}

class BehandlingDSL() {
    val steg = mutableSetOf<Steg<*>>()

    /*companion object {
        inline fun <reified B> fastsettelse(id: String) = Steg.Fastsettelse(id, Svar(null, B::class.java))
    }*/
    fun steg(block: StegDSL.() -> Steg<*>): Steg<*> {
        return block(StegDSL())
    }

    inner class StegDSL {
        inline fun <reified B> fastsettelse(id: String, avhengigheter: Avhengigheter.() -> Unit = {}) =
            Steg.Fastsettelse(id, Svar(null, B::class.java)).also {
                steg.add(it)
            }.apply {
                avhengigheter(Avhengigheter(this))
            }

        fun vilkår(id: String, avhengigheter: Avhengigheter.() -> Unit = {}) =
            Steg.Vilkår(id).also {
                steg.add(it)
            }.apply {
                avhengigheter(Avhengigheter(this))
            }

        inner class Avhengigheter(val avhengigSteg: Steg<*>) {
            inline fun <reified T> avhengerAvFastsettelse(id: String, block: Avhengigheter.() -> Unit = {}) {
                val fastsettelse = fastsettelse<T>(id)
                block(Avhengigheter(fastsettelse))
                avhengigSteg.avhengerAv(fastsettelse)
            }

            fun avhengerAv(steg: Steg<*>) {
                avhengigSteg.avhengerAv(steg)
            }

            fun avhengerAvVilkår(id: String, block: Avhengigheter.() -> Unit = {}) {
                val vilkår = Steg.Vilkår(id)
                block(Avhengigheter(vilkår))
                avhengigSteg.avhengerAv(vilkår)
            }
        }
    }
}
