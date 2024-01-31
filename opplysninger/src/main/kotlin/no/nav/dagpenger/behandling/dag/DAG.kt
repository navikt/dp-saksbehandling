package no.nav.dagpenger.behandling.dag

class Node<T>(val name: String, val data: T)

class Edge<T>(val from: Node<T>, val to: Node<T>, val edgeName: String)

class DAG<T>(val nodes: List<Node<T>>, val edges: List<Edge<T>>) {
    fun findLeafNodes(): List<Node<T>> {
        val outgoingNodes = edges.map { it.from }
        return nodes.filterNot { it in outgoingNodes }
    }
}
