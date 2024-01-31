package no.nav.dagpenger.behandling.dag

import no.nav.dagpenger.behandling.Opplysningstype

class DAGPrettyPrint(private val dag: DAG<*>) {
    fun prettyPrint(root: Opplysningstype<Boolean>) {
        val adjacencyList = dag.edges.groupBy { it.from }
        val visitedNodes = mutableSetOf<Node<*>>()

        fun printNode(
            node: Node<*>,
            indent: Int,
        ) {
            if (node !in visitedNodes) {
                visitedNodes.add(node)
                println("  ".repeat(indent) + "${node.name}: ${node.data}")

                if (node in adjacencyList.keys) {
                    for (edge in adjacencyList[node]!!) {
                        println("  ".repeat(indent + 1) + "| ${edge.edgeName}")
                        printNode(edge.to, indent + 1)
                    }
                }
            }
        }

        printNode(dag.nodes.first { it.data == root }, 0)
    }
}
