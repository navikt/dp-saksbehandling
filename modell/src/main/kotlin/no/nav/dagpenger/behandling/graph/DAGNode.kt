package no.nav.dagpenger.behandling.graph

class DAGNode<T>(val value: T) {
    private val parents = mutableListOf<DAGNode<T>>()
    private val children = mutableListOf<DAGNode<T>>()

    fun addChild(child: DAGNode<T>) {
        children.add(child)
        child.parents.add(this)
    }

    fun getDescendants(): List<DAGNode<T>> = getDescendants { true }

    fun getAncestors(): List<DAGNode<T>> {
        val allAncestors = mutableListOf<DAGNode<T>>()
        for (parent in parents) {
            allAncestors.add(parent)
            allAncestors.addAll(parent.getAncestors())
        }
        return allAncestors
    }

    fun getDescendants(criteria: (T) -> Boolean): List<DAGNode<T>> = getDescendants(true, criteria)

    fun getDescendants(recursive: Boolean, criteria: (T) -> Boolean): List<DAGNode<T>> {
        val allDescendants = mutableListOf<DAGNode<T>>()
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
}
