package no.nav.dagpenger.behandling.graph

class TreeNode<T>(val value: T) {
    private val children: MutableSet<TreeNode<T>> = mutableSetOf()
    private var parent: TreeNode<T>? = null

    fun addChild(child: TreeNode<T>) {
        children.add(child)
        child.parent = this
    }

    fun getAllNodes(traversalOrder: TraversalOrder = TraversalOrder.PRE_ORDER): List<TreeNode<T>> {
        return when (traversalOrder) {
            TraversalOrder.PRE_ORDER -> preOrderTraversal()
            TraversalOrder.POST_ORDER -> postOrderTraversal()
        }
    }

    fun getAllParents(): Set<TreeNode<T>> {
        val parents = mutableSetOf<TreeNode<T>>()
        var currentNode = this.parent
        while (currentNode != null) {
            parents.add(currentNode)
            currentNode = currentNode.parent
        }
        return parents
    }

    fun getAllNodesWithCriteria(criteria: (T) -> Boolean): List<TreeNode<T>> {
        val result = mutableListOf<TreeNode<T>>()
        if (criteria(value)) {
            result.add(this)
            children.forEach { result.addAll(it.getAllNodesWithCriteria(criteria)) }
        }
        return result
    }

    private fun preOrderTraversal(): List<TreeNode<T>> {
        val result = mutableListOf<TreeNode<T>>()
        result.add(this)
        children.forEach { result.addAll(it.preOrderTraversal()) }
        return result
    }

    private fun postOrderTraversal(): List<TreeNode<T>> {
        val result = mutableListOf<TreeNode<T>>()
        children.forEach { result.addAll(it.postOrderTraversal()) }
        result.add(this)
        return result
    }
}

enum class TraversalOrder {
    PRE_ORDER,
    POST_ORDER,
}