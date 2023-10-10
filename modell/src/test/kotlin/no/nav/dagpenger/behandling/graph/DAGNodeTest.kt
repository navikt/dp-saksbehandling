package no.nav.dagpenger.behandling.graph

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class DAGNodeTest {
    private val farfar = DAGNode("farfar")
    private val far = DAGNode("far")
    private val mor = DAGNode("mor")
    private val barn1 = DAGNode("barn1")
    private val barn2 = DAGNode("barn2")

    @Test
    fun `add child without creating a cycle`() {
        shouldNotThrow<IllegalStateException> {
            farfar.addChild(far)
            far.addChild(barn1)
        }
    }

    @Test
    fun `attempt to create a cycle should throw exception`() {
        farfar.addChild(far)
        far.addChild(barn1)

        shouldThrow<IllegalStateException> {
            farfar.addChild(farfar)
        }
        shouldThrow<IllegalStateException> {
            barn1.addChild(farfar)
        }
    }

    private fun createFamilyGraph() {
        farfar.addChildren(mor, far)
        far.addChildren(barn1, barn2)
        mor.addChildren(barn1, barn2)
    }

    @Test
    fun `descendants methods`() {
        createFamilyGraph()

        barn1.getDescendants() shouldBe emptySet()
        far.getDescendants() shouldContainExactlyInAnyOrder setOf(barn1, barn2)
        mor.getDescendants() shouldContainExactlyInAnyOrder setOf(barn1, barn2)

        farfar.getDescendants() shouldContainExactlyInAnyOrder setOf(far, mor, barn1, barn2)

        farfar.getDescendants { it.startsWith("barn") } shouldContainExactlyInAnyOrder setOf(barn1, barn2)

        farfar.getDescendants(recursive = false) { it.length > 3 } shouldBe emptySet()
        farfar.getDescendants(recursive = true) { it.length > 3 } shouldContainExactlyInAnyOrder setOf(barn1, barn2)

        farfar.getDescendantsAndRoot() shouldContainExactlyInAnyOrder setOf(farfar, far, mor, barn1, barn2)
        barn1.getDescendantsAndRoot() shouldContainExactly setOf(barn1)
    }

    @Test
    fun `ancestor methods`() {
        createFamilyGraph()

        barn1.getAncestors() shouldContainExactlyInAnyOrder setOf(far, mor, farfar)
        mor.getAncestors() shouldContainExactly setOf(farfar)
        farfar.getAncestors() shouldBe emptySet()
    }
}
