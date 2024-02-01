package no.nav.dagpenger.behandling.dag.printer

import no.nav.dagpenger.behandling.Opplysningstype
import no.nav.dagpenger.behandling.dag.DAG
import no.nav.dagpenger.behandling.dag.Node

class PrettyPrinter(private val dag: DAG<*>) : DAGPrinter {
    override fun toPrint(root: Opplysningstype<Boolean>?): String {
        val adjacencyList = dag.edges.groupBy { it.from }
        val visitedNodes = mutableSetOf<Node<*>>()

        val prettyPrint = StringBuilder()

        fun printNode(
            node: Node<*>,
            indent: Int,
        ) {
            if (node !in visitedNodes) {
                visitedNodes.add(node)
                prettyPrint.appendLine("  ".repeat(indent) + "${node.name}: ${node.data}")

                if (node in adjacencyList.keys) {
                    for (edge in adjacencyList[node]!!) {
                        prettyPrint.appendLine("  ".repeat(indent + 1) + "| ${edge.edgeName}")
                        printNode(edge.to, indent + 1)
                    }
                }
            }
        }

        val startNode =
            when (root) {
                is Opplysningstype -> dag.nodes.first { it.data == root }
                else -> dag.nodes.first()
            }
        printNode(startNode, 0)

        return prettyPrint.trim().toString()
    }
}
