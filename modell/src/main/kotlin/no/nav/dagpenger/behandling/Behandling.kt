package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.Svar.Companion.Ubesvart
import no.nav.dagpenger.behandling.graph.TreeNode

class Behandling(
    val steg: Set<Steg> = emptySet(),
) {
    fun nesteSteg(): Set<Steg> {
        return steg.flatMap {
            it.nesteSteg()
        }.toSet()
    }
}

class Steg(
    val id: String,
    var svar: Svar<*> = Ubesvart,
    avhengerAv: Set<Steg> = emptySet(),
) {

    private val node: TreeNode<Steg> = TreeNode(this).also { root ->
        avhengerAv.forEach { steg ->
            root.addChild(steg.node)
        }
    }

    override fun toString() = id

    fun nesteSteg(): Set<Steg> =
        node.getAllNodesWithCriteria { it.svar == Ubesvart }
            .map { it.value }
            .toSet()

    fun besvar(svar: Svar<*>) {
        this.svar = svar
        node.getAllParents().forEach {
            it.value.nullstill()
        }
    }

    fun nullstill() {
        svar = Ubesvart
    }
}

class Svar<T>(val verdi: T) {
    companion object {
        val Ubesvart = Svar(Unit)
    }
}
