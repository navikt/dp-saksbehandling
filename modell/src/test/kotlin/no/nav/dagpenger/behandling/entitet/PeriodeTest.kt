package no.nav.dagpenger.behandling.entitet

import no.nav.dagpenger.behandling.entitet.Periode.Companion.til
import no.nav.dagpenger.behandling.hjelpere.februar
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PeriodeTest {
    @Test
    fun `Skal kunne opprette gyldige perioder`() {
        val periode = 12.februar til 14.februar
        assertFalse(11.februar in periode)
        assertTrue(12.februar in periode)
        assertTrue(13.februar in periode)
        assertTrue(14.februar in periode)
        assertFalse(15.februar in periode)

        assertThrows<IllegalArgumentException> { 14.februar til 12.februar }
        assertDoesNotThrow { 1.februar til 1.februar }
    }

    @Test
    fun `kan iterere`() {
        val periode = 12.februar til 17.februar
        val datoer = periode.map { it }
        assertEquals(
            listOf(12.februar, 13.februar, 14.februar, 15.februar, 16.februar, 17.februar),
            datoer,
        )
    }

    @Test
    fun `kan legge sammen periode`() {
        val periode1 = 12.februar til 17.februar
        val periode2 = 18.februar til 28.februar
        val merge = periode1 plus periode2
        assertEquals(12.februar, merge.start)
        assertEquals(28.februar, merge.endInclusive)
    }
}
