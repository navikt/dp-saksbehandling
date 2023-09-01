package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.graph.DAGNode
import no.nav.dagpenger.behandling.graph.DAGNodeVisitor
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

        companion object {
            inline fun <reified T> rehydrer(id: String, svar: Svar<T>): Fastsettelse<T> =
                Fastsettelse<T>(id = id, svar = svar)
        }
    }

    class Vilkår(
        id: String,
    ) : Steg<Boolean>(id = id, svar = Svar(null, Boolean::class.javaObjectType, NullSporing())) {
        override val node: DAGNode<Steg<*>> = DAGNode(this)
    }

    companion object {
        inline fun <reified B> fastsettelse(id: String) = Fastsettelse(id, Svar(null, B::class.java, NullSporing()))
    }

    protected abstract val node: DAGNode<Steg<*>>

    fun avhengerAv(steg: Steg<*>): Steg<*> {
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

    fun besvar(svar: T, sporing: Sporing) {
        this.svar = this.svar.besvar(svar, sporing)
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

    fun accept(visitor: DAGNodeVisitor) {
        node.accept(visitor)
    }
}
