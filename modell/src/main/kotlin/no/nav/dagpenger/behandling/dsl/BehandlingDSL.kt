@file:Suppress("SpellCheckingInspection")

package no.nav.dagpenger.behandling.dsl

import no.nav.dagpenger.behandling.Behandling
import no.nav.dagpenger.behandling.Person
import no.nav.dagpenger.behandling.Rolle
import no.nav.dagpenger.behandling.Sak
import no.nav.dagpenger.behandling.Steg
import no.nav.dagpenger.behandling.Steg.Prosess
import no.nav.dagpenger.behandling.Steg.Vilkår
import no.nav.dagpenger.behandling.hendelser.PersonHendelse

class BehandlingDSL() {
    val steg = mutableSetOf<Steg<*>>()

    companion object {
        fun behandling(
            person: Person,
            hendelse: PersonHendelse,
            sak: Sak = person.hentGjeldendeSak(),
            block: BehandlingDSL.() -> Unit,
        ): Behandling {
            val dsl = BehandlingDSL()
            block(dsl)
            return Behandling(person, hendelse, dsl.steg, sak)
        }
    }

    fun steg(block: StegDSL.() -> Steg<*>): Steg<*> {
        return block(StegDSL()).also {
            steg.add(it)
        }
    }

    inner class StegDSL {
        inline fun <reified B> fastsettelse(id: String, konfigurasjon: Konfigurasjon.() -> Unit = {}) =
            Steg.fastsettelse<B>(id).also {
                konfigurasjon(Konfigurasjon(it))
            }

        fun vilkår(id: String, konfigurasjon: Konfigurasjon.() -> Unit = {}) =
            Vilkår(id).also {
                konfigurasjon(Konfigurasjon(it))
            }

        fun prosess(id: String, konfigurasjon: Konfigurasjon.() -> Unit = {}) =
            Prosess(id).also {
                konfigurasjon(Konfigurasjon(it))
            }

        inner class Konfigurasjon(private val avhengigSteg: Steg<*>) {
            var rolle: Rolle = Rolle.Saksbehandler

            inline fun <reified T> avhengerAvFastsettelse(id: String, block: Konfigurasjon.() -> Unit = {}) {
                val fastsettelse = fastsettelse<T>(id)
                block(Konfigurasjon(fastsettelse))
                avhengerAv(fastsettelse)
            }

            fun avhengerAvVilkår(id: String, block: Konfigurasjon.() -> Unit = {}) {
                val vilkår = Vilkår(id)
                block(Konfigurasjon(vilkår))
                avhengerAv(vilkår)
            }

            fun avhengerAv(steg: Steg<*>) {
                avhengigSteg.avhengerAv(steg)
            }
        }
    }
}
