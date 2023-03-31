package no.nav.dagpenger.behandling.graph

class DAGNode<T>(val value: T) {
    private val parents = mutableSetOf<DAGNode<T>>()
    private val children = mutableSetOf<DAGNode<T>>()

    fun addChild(child: DAGNode<T>) {
        if (this == child || this in child.getDescendants()) {
            throw IllegalStateException("Adding this child would create a cycle")
        } else {
            children.add(child)
            child.parents.add(this)
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

    fun getDescendants(recursive: Boolean, criteria: (T) -> Boolean): Set<DAGNode<T>> {
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
}
