package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.graph.TreeNode

class Steg(
    val id: String,
    var svar: Svar<*> = Svar.Ubesvart,
    avhengerAv: Set<Steg> = emptySet(),
) : StegDeklerasjon {

    private val node: TreeNode<Steg> = TreeNode(this).also { root ->
        avhengerAv.forEach { steg ->
            root.addChild(steg.node)
        }
    }

    override fun avhengerAv(id: String, svar: Svar<*>, avhengerAv: AvhengerAv?): Steg {
        return Steg(id, svar).also { nyttSteg ->
            node.addChild(nyttSteg.node)
            avhengerAv?.invoke(nyttSteg)
        }
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
