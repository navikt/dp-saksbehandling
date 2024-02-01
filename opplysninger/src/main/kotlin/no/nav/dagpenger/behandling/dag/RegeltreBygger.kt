package no.nav.dagpenger.behandling.dag

import no.nav.dagpenger.behandling.Opplysningstype
import no.nav.dagpenger.behandling.Regelsett
import no.nav.dagpenger.behandling.regel.Regel

class RegeltreBygger(private val regler: List<Regel<*>>) {
    constructor(regelsett: Regelsett) : this(regelsett.regler())

    private val nodes = mutableMapOf<Opplysningstype<*>, Node<Opplysningstype<*>>>()
    private val edges = mutableListOf<Edge<Opplysningstype<*>>>()

    fun dag(): DAG<Opplysningstype<*>> {
        regler.forEach { addRegel(it) }
        return DAG(nodes.values.toList(), edges.toList())
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
