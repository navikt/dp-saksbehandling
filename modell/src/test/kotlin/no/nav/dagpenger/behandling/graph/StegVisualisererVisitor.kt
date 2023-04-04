package no.nav.dagpenger.behandling.graph

import java.io.File

internal class StegVisualisererVisitor : DAGNodeVisitor {

    private val edges = mutableListOf<String>()

    override fun <T> visit(node: T, children: Set<DAGNode<T>>, parents: Set<DAGNode<T>>) {
        children.forEach { steg ->
            edges.add("$node --> $steg")
        }
    }

    fun asDiagram(): String = """
        |graph TD;
        |${edges.joinToString("\n")}
    """.trimMargin()

    fun toMarkdownFile(filename: String = "stegVisualisererResultat.md", writeToFile: Boolean = true): String {
        // language=markdown
        val contents = """```mermaid
        |${asDiagram()}
        |```
        """.trimMargin()
        if (writeToFile) {
            File(filename).writeText(contents)
        }
        return contents
    }
}