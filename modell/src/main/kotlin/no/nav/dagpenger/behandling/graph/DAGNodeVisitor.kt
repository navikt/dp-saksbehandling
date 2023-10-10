package no.nav.dagpenger.behandling.graph

interface DAGNodeVisitor {
    fun <T> visit(
        node: T,
        children: Set<DAGNode<T>>,
        parents: Set<DAGNode<T>>,
    )
}
