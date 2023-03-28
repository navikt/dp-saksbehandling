@file:Suppress("SpellCheckingInspection")

package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.Steg.Fastsettelse
import no.nav.dagpenger.behandling.Steg.Vilkår

fun behandling(person: Person, block: BehandlingDSL.() -> Unit): Behandling {
    val dsl = BehandlingDSL()
    block(dsl)
    return Behandling(person, dsl.steg)
}

class BehandlingDSL() {
    val steg = mutableSetOf<Steg<*>>()

    fun steg(block: StegDSL.() -> Steg<*>): Steg<*> {
        return block(StegDSL())
    }

    inner class StegDSL {
        inline fun <reified B> fastsettelse(id: String, avhengigheter: Avhengigheter.() -> Unit = {}) =
            Fastsettelse(id, Svar(null, B::class.java)).also {
                steg.add(it)
                avhengigheter(Avhengigheter(it))
            }

        fun vilkår(id: String, avhengigheter: Avhengigheter.() -> Unit = {}) =
            Vilkår(id).also {
                steg.add(it)
                avhengigheter(Avhengigheter(it))
            }

        inner class Avhengigheter(private val avhengigSteg: Steg<*>) {
            inline fun <reified T> avhengerAvFastsettelse(id: String, block: Avhengigheter.() -> Unit = {}) {
                val fastsettelse = fastsettelse<T>(id)
                block(Avhengigheter(fastsettelse))
                avhengerAv(fastsettelse)
            }

            fun avhengerAvVilkår(id: String, block: Avhengigheter.() -> Unit = {}) {
                val vilkår = Vilkår(id)
                block(Avhengigheter(vilkår))
                avhengerAv(vilkår)
            }

            fun avhengerAv(steg: Steg<*>) {
                avhengigSteg.avhengerAv(steg)
            }
        }
    }
}
