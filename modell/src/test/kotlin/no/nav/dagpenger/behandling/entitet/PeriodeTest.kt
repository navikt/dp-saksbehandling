package no.nav.dagpenger.behandling.entitet

import no.nav.dagpenger.behandling.entitet.Periode.Companion.til
import no.nav.dagpenger.behandling.hjelpere.februar
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
    }
}
