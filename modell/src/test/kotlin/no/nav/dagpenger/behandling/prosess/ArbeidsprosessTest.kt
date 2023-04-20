package no.nav.dagpenger.behandling.prosess

import io.kotest.assertions.throwables.shouldThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ArbeidsprosessTest {

    @Test
    fun testStart() {
        val wp = Arbeidsprosess()
        wp.start("A")
        assertEquals("A", wp.tilstand())
    }

    @Test()
    fun testTransitionUnstarted() {
        val wp = Arbeidsprosess()
        shouldThrow<java.lang.IllegalStateException> {
            wp.gåTil("B")
        }
    }

    @Test
    fun testInvalidCurrentState() {
        val wp = Arbeidsprosess()
        wp.leggTilTilstand("A", listOf(Arbeidsprosess.Overgang("B")))
        wp.start("A")
        shouldThrow<java.lang.IllegalStateException> {
            wp.gåTil("C")
        }
    }

    @Test
    fun testInvalidTransition() {
        val wp = Arbeidsprosess()
        wp.leggTilTilstand("A", listOf(Arbeidsprosess.Overgang("B")))
        wp.leggTilTilstand("B", listOf(Arbeidsprosess.Overgang("C")))
        wp.start("A")
        shouldThrow<java.lang.IllegalStateException> {
            wp.gåTil("C")
        }
    }

    @Test
    fun testValidTransitions() {
        val wp = Arbeidsprosess()
        wp.leggTilTilstand(
            "A",
            listOf(
                Arbeidsprosess.Overgang("B"),
                Arbeidsprosess.Overgang("C") { false },
            ),
        )
        wp.leggTilTilstand(
            "B",
            listOf(
                Arbeidsprosess.Overgang("C"),
            ),
        )
        wp.start("A")
        assertEquals(listOf("B"), wp.validTransitions())
        wp.gåTil("B")
        assertEquals(listOf("C"), wp.validTransitions())
    }
}
