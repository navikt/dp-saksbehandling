package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.graph.TreeNode
import java.util.UUID

sealed class Steg<T>(
    val uuid: UUID = UUID.randomUUID(),
    val id: String,
    var svar: Svar<T>,
) {
    class Fastsettelse<T>(
        id: String,
        svar: Svar<T>,
    ) : Steg<T>(id = id, svar = svar) {
        override val node: TreeNode<Steg<*>> = TreeNode(this)
    }

    class Vilkår(
        id: String,
    ) : Steg<Boolean>(id = id, svar = Svar(null, Boolean::class.java)) {
        override val node: TreeNode<Steg<*>> = TreeNode(this)
    }

    companion object {
        inline fun <reified B> fastsettelse(id: String) = Fastsettelse(id, Svar(null, B::class.java))
    }

    protected abstract val node: TreeNode<Steg<*>>

    fun avhengerAv(steg: Steg<*>): Steg<*> {
        node.addChild(steg.node)
        return steg
    }

    override fun toString() = id

    fun nesteSteg(): Set<Steg<*>> =
        node.findNodes { it.svar.ubesvart }
            .map { it.value }
            .toSet()

    fun alleSteg(): Set<Steg<*>> = node.traverse().map { it.value }.toSet()

    fun besvar(svar: T) {
        this.svar = this.svar.besvar(svar)
        node.getAncestors().forEach {
            it.value.nullstill()
        }
    }

    private fun nullstill() {
        svar = this.svar.nullstill()
    }
}
