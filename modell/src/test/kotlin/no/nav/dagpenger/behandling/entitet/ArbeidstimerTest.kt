package no.nav.dagpenger.behandling.entitet

import no.nav.dagpenger.behandling.entitet.Arbeidstimer.Companion.arbeidstimer
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ArbeidstimerTest {

    @Test
    fun `likhet og konsistens`() {
        val arbeidstimer = 8.arbeidstimer
        assertEquals(arbeidstimer, arbeidstimer)

        assertEquals(arbeidstimer.hashCode(), arbeidstimer.hashCode())
        assertNotEquals(arbeidstimer, 7.arbeidstimer)
        assertNotEquals(arbeidstimer, Any())
        assertNotEquals(arbeidstimer, null)
        assertEquals(7.5.arbeidstimer, 7.5.arbeidstimer)

        assertDoesNotThrow { 0.arbeidstimer }
        assertThrows<IllegalArgumentException> { (-1).arbeidstimer }
    }

    @Test
    fun `kan finne arbeidsprosent`() {
        val fastsatt = 8.arbeidstimer
        assertEquals(Arbeidsprosent(50.0), 4.arbeidstimer.div(fastsatt))
    }
}
