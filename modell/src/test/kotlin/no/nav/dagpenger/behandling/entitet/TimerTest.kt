package no.nav.dagpenger.behandling.entitet

import no.nav.dagpenger.behandling.entitet.Timer.Companion.timer
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TimerTest {

    @Test
    fun `likhet og konsistens`() {
        val arbeidstimer = 8.timer
        assertEquals(arbeidstimer, arbeidstimer)

        assertEquals(arbeidstimer.hashCode(), arbeidstimer.hashCode())
        assertNotEquals(arbeidstimer, 7.timer)
        assertNotEquals(arbeidstimer, Any())
        assertNotEquals(arbeidstimer, null)
        assertEquals(7.5.timer, 7.5.timer)

        assertDoesNotThrow { 0.timer }
        assertThrows<IllegalArgumentException> { (-1).timer }
    }

    @Test
    fun `kan finne arbeidsprosent`() {
        val fastsatt = 8.timer
        assertEquals(Prosent(50.0), 4.timer.div(fastsatt))
        assertEquals(Prosent(0), 0.timer.div(fastsatt))
    }
}
