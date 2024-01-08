package no.nav.dagpenger.behandling

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OpplysningstypeTest {
    @Test
    fun `enkle opplysningstyper`() {
        val fødselsdato = Opplysningstype("Fødselsdato")
        assertTrue(fødselsdato.er(fødselsdato))
    }

    @Test
    fun `hierarkiske opplysningstyper`() {
        val vilkår = Opplysningstype("Vilkår")
        val minsteinntekt = Opplysningstype("Minsteinntekt", vilkår)

        assertTrue(minsteinntekt.er(minsteinntekt))
        assertTrue(minsteinntekt.er(vilkår))

        assertEquals(listOf(minsteinntekt), vilkår.bestårAv())
    }
}
