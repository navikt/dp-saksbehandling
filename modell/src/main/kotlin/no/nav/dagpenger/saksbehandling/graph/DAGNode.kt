package no.nav.dagpenger.saksbehandling.graph

interface Edge<T> {
    val from: DAGNode<T>
    val to: DAGNode<T>
}

class SimpleEdge<T>(override val from: DAGNode<T>, override val to: DAGNode<T>) : Edge<T>

class DAGNode<T>(val value: T) {
    private val incomingEdges = mutableSetOf<Edge<T>>()
    private val outgoingEdges = mutableSetOf<Edge<T>>()

    private val parents
        get() = incomingEdges.map { it.from }.toSet()
    private val children
        get() = outgoingEdges.map { it.to }.toSet()

    fun children(): Set<DAGNode<T>> = children

    fun addChild(child: DAGNode<T>) {
        if (this == child || this in child.getDescendants()) {
            throw IllegalStateException("Adding this child would create a cycle: $child")
        } else {
            val edge = SimpleEdge(this, child)
            outgoingEdges.add(edge)
            child.incomingEdges.add(edge)
        }
    }

    fun addChildren(vararg children: DAGNode<T>) = children.forEach { child -> addChild(child) }

    fun getDescendants(): Set<DAGNode<T>> = getDescendants { true }

    fun getAncestors(): Set<DAGNode<T>> {
        val allAncestors = mutableSetOf<DAGNode<T>>()
        for (parent in parents) {
            allAncestors.add(parent)
            allAncestors.addAll(parent.getAncestors())
        }
        return allAncestors
    }

    fun getDescendantsAndRoot(): Set<DAGNode<T>> = setOf(this) + getDescendants()

    fun getDescendants(criteria: (T) -> Boolean): Set<DAGNode<T>> = getDescendants(true, criteria)

    fun getDescendants(
        recursive: Boolean,
        criteria: (T) -> Boolean,
    ): Set<DAGNode<T>> {
        val allDescendants = mutableSetOf<DAGNode<T>>()
        for (child in children) {
            when {
                criteria(child.value) -> {
                    allDescendants.add(child)
                    allDescendants.addAll(child.getDescendants(criteria))
                }

                recursive -> {
                    allDescendants.addAll(child.getDescendants(criteria))
                }
            }
        }
        return allDescendants
    }

    override fun toString(): String = value.toString()

    fun accept(visitor: DAGNodeVisitor) {
        visitor.visit(value, children, parents)
        children.forEach { child ->
            child.accept(visitor)
        }
    }
}
