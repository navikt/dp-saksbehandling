package no.nav.dagpenger.behandling.dag

class MermaidDiagramBuilder(private val dag: DAG<*>) {
    fun toMermaidDiagram(): String {
        val diagram = StringBuilder()

        diagram.appendLine("graph RL")

        dag.edges.forEach { edge ->
            val fromNodeName = "${edge.from.hashCode()}[\"${edge.from.name}\"]"
            val toNodeName = "${edge.to.hashCode()}[\"${edge.to.name}\"]"
            diagram.appendLine("  $fromNodeName -->|${edge.edgeName}| $toNodeName")
        }

        return diagram.toString()
    }
}
