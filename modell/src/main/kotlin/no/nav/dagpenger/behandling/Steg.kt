package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.graph.TreeNode

sealed class Steg<T>(
    val id: String,
    var svar: Svar<T>,
) {
    class Fastsettelse<T>(
        id: String,
        svar: Svar<T>,
    ) : Steg<T>(id, svar) {
        override val node: TreeNode<Steg<*>> = TreeNode(this)
    }

    companion object {
        inline fun <reified B> fastsettelse(id: String) = Fastsettelse(id, Svar(null, B::class.java))
    }

    class Vilkår(
        id: String,
    ) : Steg<Boolean>(id, Svar(null, Boolean::class.java)) {
        override val node: TreeNode<Steg<*>> = TreeNode(this)
    }

    protected abstract val node: TreeNode<Steg<*>>

    /*override fun <T> avhengerAvFastsettelse(id: String, svar: Svar<T>, avhengerAv: AvhengerAv?): Steg<T> {
        return FastSettelse(id, svar).also { nyttSteg ->
            avhengerAv(nyttSteg, avhengerAv)
        }
    }

    override fun avhengerAvVilkår(id: String, svar: Svar<Boolean>, avhengerAv: AvhengerAv?): Steg<Boolean> {
        return Vilkår(id, svar).also { avhengerAv(it, avhengerAv) }
    }*/
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

    fun nullstill() {
        svar = this.svar.nullstill()
    }
}
