@file:Suppress("SpellCheckingInspection")

package no.nav.dagpenger.behandling.dsl

import no.nav.dagpenger.behandling.Behandling
import no.nav.dagpenger.behandling.Person
import no.nav.dagpenger.behandling.Steg
import no.nav.dagpenger.behandling.Steg.Vilkår
import no.nav.dagpenger.behandling.hendelser.Hendelse

class BehandlingDSL() {
    val steg = mutableSetOf<Steg<*>>()

    companion object {
        fun behandling(person: Person, hendelse: Hendelse, block: BehandlingDSL.() -> Unit): Behandling {
            val dsl = BehandlingDSL()
            block(dsl)
            return Behandling(person, hendelse, dsl.steg)
        }
    }

    fun steg(block: StegDSL.() -> Steg<*>): Steg<*> {
        return block(StegDSL()).also {
            steg.add(it)
        }
    }

    inner class StegDSL {
        inline fun <reified B> fastsettelse(id: String, avhengigheter: Avhengigheter.() -> Unit = {}) =
            Steg.fastsettelse<B>(id).also {
                avhengigheter(Avhengigheter(it))
            }

        fun vilkår(id: String, avhengigheter: Avhengigheter.() -> Unit = {}) =
            Vilkår(id).also {
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
