package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.graph.TreeNode

sealed class Steg(
    val id: String,
    var svar: Svar<*> = Svar.Ubesvart,
) : StegDeklerasjon {

    class FastSettelse(
        id: String,
        svar: Svar<*> = Svar.Ubesvart,
    ) : Steg(id, svar) {
        override val node: TreeNode<Steg> = TreeNode(this)
    }

    class Vilkår(
        id: String,
        svar: Svar<*> = Svar.Ubesvart,
    ) : Steg(id, svar) {
        override val node: TreeNode<Steg> = TreeNode(this)
    }

    protected abstract val node: TreeNode<Steg>

    override fun avhengerAvFastsettelse(id: String, svar: Svar<*>, avhengerAv: AvhengerAv?): Steg {
        return FastSettelse(id, svar).also { nyttSteg ->
            avhengerAv(nyttSteg, avhengerAv)
        }
    }

    override fun avhengerAvVilkår(id: String, svar: Svar<*>, avhengerAv: AvhengerAv?): Steg {
        return Vilkår(id, svar).also { avhengerAv(it, avhengerAv) }
    }

    override fun avhengerAv(steg: Steg, avhengerAv: AvhengerAv?): Steg {
        node.addChild(steg.node)
        avhengerAv?.invoke(steg)
        return steg
    }

    override fun toString() = id

    fun nesteSteg(): Set<Steg> =
        node.findNodes { it.svar == Svar.Ubesvart }
            .map { it.value }
            .toSet()

    fun alleSteg(): Set<Steg> = node.traverse().map { it.value }.toSet()

    fun besvar(svar: Svar<*>) {
        this.svar = svar
        node.getAncestors().forEach {
            it.value.nullstill()
        }
    }

    fun nullstill() {
        svar = Svar.Ubesvart
    }
}
