package no.nav.dagpenger.behandling.graph

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TreeNodeTest {

    @Test
    fun `add child`() {
        val parent = TreeNode("parent")
        val child = TreeNode("child")
        parent.addChild(child)
        assertEquals(listOf(parent, child), parent.traverse())
    }

    @Test
    fun `get ancestors`() {
        val grandparent = TreeNode("grandparent")
        val parent = TreeNode("parent")
        val child = TreeNode("child")
        grandparent.addChild(parent)
        parent.addChild(child)
        assertEquals(listOf(parent, grandparent), child.getAncestors())
    }

    @Test
    fun `find nodes`() {
        val parent = TreeNode("parent")
        val child1 = TreeNode("child1")
        val child2 = TreeNode("child2")
        parent.addChild(child1)
        parent.addChild(child2)

        assertEquals(emptyList<TreeNode<String>>(), parent.findNodes { it.contains("child") })
        assertEquals(listOf(parent, child1, child2), parent.findNodes { true })
    }

    @Test
    fun `traverse tree`() {
        val root = TreeNode("root")
        val child1 = TreeNode("child1")
        val child2 = TreeNode("child2")
        val grandchild = TreeNode("grandchild")
        root.addChild(child1)
        root.addChild(child2)
        child2.addChild(grandchild)
        val preOrderTraversal = root.traverse(TraversalOrder.PRE_ORDER)
        val postOrderTraversal = root.traverse(TraversalOrder.POST_ORDER)
        assertEquals(listOf(root, child1, child2, grandchild), preOrderTraversal)
        assertEquals(listOf(child1, grandchild, child2, root), postOrderTraversal)
    }
}
