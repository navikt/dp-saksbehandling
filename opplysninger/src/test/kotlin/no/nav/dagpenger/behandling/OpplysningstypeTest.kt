package no.nav.dagpenger.behandling

import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.equals.shouldNotBeEqual
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OpplysningstypeTest {
    @Test
    fun `likhet test`() {
        val fødselsdato = Opplysningstype<LocalDate>("Fødselsdato")
        fødselsdato shouldBeEqual fødselsdato
        fødselsdato.hashCode() shouldBeEqual fødselsdato.hashCode()
        Opplysningstype<LocalDate>("Fødselsdato") shouldBeEqual fødselsdato
        fødselsdato shouldNotBeEqual Any()
    }

    @Test
    fun `enkle opplysningstyper`() {
        val fødselsdato = Opplysningstype<LocalDate>("Fødselsdato")
        assertTrue(fødselsdato.er(fødselsdato))
    }

    @Test
    fun `hierarkiske opplysningstyper`() {
        val vilkår = Opplysningstype<LocalDate>("Vilkår")
        val minsteinntekt = Opplysningstype("Minsteinntekt", vilkår)

        assertTrue(minsteinntekt.er(minsteinntekt))
        assertTrue(minsteinntekt.er(vilkår))

        assertEquals(listOf(minsteinntekt), vilkår.bestårAv())
    }
}
