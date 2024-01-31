package no.nav.dagpenger.behandling.dag

import no.nav.dagpenger.behandling.Opplysningstype
import no.nav.dagpenger.behandling.regel.Regel

class RegeltreBygger(regler: List<Regel<*>>) {
    private val nodes = mutableMapOf<Opplysningstype<*>, Node<Opplysningstype<*>>>()
    private val edges = mutableListOf<Edge<Opplysningstype<*>>>()

    init {
        regler.forEach { addRegel(it) }
    }

    fun byggDAG(): DAG<Opplysningstype<*>> {
        return DAG(nodes.values.toList(), edges)
    }

    private fun addRegel(regel: Regel<*>) {
        val currentNode = getOrCreateNode(regel.produserer)

        regel.avhengerAv.forEach { dependency ->
            val dependencyNode = getOrCreateNode(dependency)
            edges.add(Edge(from = currentNode, to = dependencyNode, edgeName = regel.javaClass.simpleName))
        }
    }

    private fun getOrCreateNode(opplysningstype: Opplysningstype<*>): Node<Opplysningstype<*>> {
        return nodes.computeIfAbsent(opplysningstype) { Node(it.navn, it) }
    }
}
