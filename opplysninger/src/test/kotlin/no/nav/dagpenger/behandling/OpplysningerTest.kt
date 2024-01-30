package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.regel.enAvRegel
import no.nav.dagpenger.behandling.regel.multiplikasjon
import no.nav.dagpenger.behandling.regel.størreEnn
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class OpplysningerTest {
    @Test
    fun `vilkår er avhengig av andre vilkår`() {
        val vilkår = Opplysningstype<Boolean>("Vilkår")
        val minsteinntekt = Opplysningstype("Minsteinntekt", vilkår)
        val alder = Opplysningstype("Alder", vilkår)

        val opplysninger = Opplysninger(Regelmotor())

        opplysninger.leggTil(Faktum(minsteinntekt, true))
        opplysninger.leggTil(Faktum(alder, true))
        opplysninger.leggTil(Faktum(vilkår, true))

        assertEquals(3, opplysninger.size)
        assertTrue(opplysninger.har(minsteinntekt))
        assertTrue(opplysninger.har(alder))
        assertTrue(opplysninger.har(vilkår))
    }

    @Test
    fun `finn alle løvnoder som mangler`() {
        val vilkår = Opplysningstype<Boolean>("Vilkår")

        val nedreTerskelFaktor = Opplysningstype<Double>("Nedre inntektsterskel")
        val øvreTerskelFaktor = Opplysningstype<Double>("Øvre inntektsterskel")
        val inntekt = Opplysningstype<Double>("Inntekt")
        val grunnbeløp = Opplysningstype<Double>("Grunnbeløp")

        val regelmotor = Regelmotor()

        val nedreTerskel = Opplysningstype<Double>("Nedre terskel")
        regelmotor.multiplikasjon(nedreTerskel, nedreTerskelFaktor, grunnbeløp)

        val øvreTerskel = Opplysningstype<Double>("Øvre terskel")
        regelmotor.multiplikasjon(øvreTerskel, øvreTerskelFaktor, grunnbeløp)

        val overNedreTerskel = Opplysningstype<Boolean>("Inntekt er over nedre terskel")
        regelmotor.størreEnn(overNedreTerskel, inntekt, nedreTerskel)

        val overØvreTerskel = Opplysningstype<Boolean>("Inntekt er over øvre terskel")
        regelmotor.størreEnn(overØvreTerskel, inntekt, øvreTerskel)

        val minsteinntekt = Opplysningstype("Minsteinntekt", vilkår)
        regelmotor.enAvRegel(minsteinntekt, overNedreTerskel, overØvreTerskel)

        val opplysninger = Opplysninger(regelmotor)

        // Finn alle opplysninger som ikke kan utledes (har ikke andre avhengigheter) og mangler
        val actual = opplysninger.trenger(minsteinntekt)
        assertEquals(4, actual.size)
        assertEquals(listOf(inntekt, nedreTerskelFaktor, grunnbeløp, øvreTerskelFaktor), actual)

        opplysninger.leggTil(Faktum(nedreTerskelFaktor, 1.5))
        assertEquals(3, opplysninger.trenger(minsteinntekt).size)

        opplysninger.leggTil(Faktum(øvreTerskelFaktor, 3.0))
        assertEquals(2, opplysninger.trenger(minsteinntekt).size)

        opplysninger.leggTil(Faktum(inntekt, 321321.0))
        assertEquals(1, opplysninger.trenger(minsteinntekt).size)

        opplysninger.leggTil(Faktum(grunnbeløp, 118620.0))
        assertEquals(0, opplysninger.trenger(minsteinntekt).size)

        assertTrue(opplysninger.har(minsteinntekt))
        assertTrue(opplysninger.find { it.er(minsteinntekt) }?.verdi as Boolean)

        opplysninger.leggTil(Faktum(minsteinntekt, false))

        opplysninger.forEach { println(it) }
        assertTrue(opplysninger.har(minsteinntekt))
        assertFalse(opplysninger.findLast { it.er(minsteinntekt) }?.verdi as Boolean)

        opplysninger.forEach { println(it) }
    }
}
