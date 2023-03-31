package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.graph.DAGNode
import java.util.UUID

sealed class Steg<T> private constructor(
    val uuid: UUID = UUID.randomUUID(),
    val id: String,
    var svar: Svar<T>,
    var tilstand: Tilstand = Tilstand.IkkeUtført,
) {
    class Fastsettelse<T>(
        id: String,
        svar: Svar<T>,
    ) : Steg<T>(id = id, svar = svar) {
        override val node: DAGNode<Steg<*>> = DAGNode(this)
    }

    class Vilkår(
        id: String,
    ) : Steg<Boolean>(id = id, svar = Svar(null, Boolean::class.javaObjectType)) {
        override val node: DAGNode<Steg<*>> = DAGNode(this)
    }

    companion object {
        inline fun <reified B> fastsettelse(id: String) = Fastsettelse(id, Svar(null, B::class.java))
    }

    protected abstract val node: DAGNode<Steg<*>>

    internal fun avhengerAv(steg: Steg<*>): Steg<*> {
        node.addChild(steg.node)
        return steg
    }

    override fun toString() = id

    fun nesteSteg(): Set<Steg<*>> {
        val criteria: (steg: Steg<*>) -> Boolean = { it.tilstand != Tilstand.Utført }
        val result = mutableSetOf<Steg<*>>()
        if (criteria(this)) {
            val x = node.getDescendants(false, criteria).map { it.value }
            if (x.isEmpty()) {
                result.add(this)
            } else {
                result.addAll(x)
            }
        }
        return result
    }

    fun alleSteg(): Set<Steg<*>> = setOf(this) + node.getDescendants().map { it.value }

    fun besvar(svar: T) {
        this.svar = this.svar.besvar(svar)
        this.tilstand = Tilstand.Utført
        node.getAncestors().forEach {
            it.value.måGodkjennes()
        }
    }

    private fun måGodkjennes() {
        if (tilstand == Tilstand.Utført) {
            this.tilstand = Tilstand.MåGodkjennes
        }
    }
}
