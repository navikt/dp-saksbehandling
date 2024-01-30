package no.nav.dagpenger.behandling

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class OpplysningerTest {
    @Test
    fun `vilkår er avhengig av andre vilkår`() {
        val vilkår = Opplysningstype<Boolean>("Vilkår")
        val minsteinntekt = Opplysningstype("Minsteinntekt", vilkår)
        val alder = Opplysningstype("Alder", vilkår)

        val opplysninger = Opplysninger()

        opplysninger.leggTil(Faktum(minsteinntekt, true))
        opplysninger.leggTil(Faktum(alder, true))
        opplysninger.leggTil(Faktum(vilkår, true))

        assertEquals(3, opplysninger.size)
        assertTrue(opplysninger.har(minsteinntekt))
        assertTrue(opplysninger.har(alder))
    }

    @Test
    fun `finn alle løvnoder som mangler`() {
        val vilkår = Opplysningstype<Boolean>("Vilkår")

        val nedreTerskelFaktor = Opplysningstype<Double>("Nedre inntektsterskel")
        val øvreTerskelFaktor = Opplysningstype<Double>("Øvre inntektsterskel")
        val inntekt = Opplysningstype<Double>("Inntekt")
        val grunnbeløp = Opplysningstype<Double>("Grunnbeløp")

        // Disse bør få utledesAv fra reglene
        val nedreTerskel =
            Opplysningstype<Double>("Nedre terskel", utledesAv = mutableSetOf(nedreTerskelFaktor, grunnbeløp))
        val øvreTerskel =
            Opplysningstype<Double>("Øvre terskel", utledesAv = mutableSetOf(øvreTerskelFaktor, grunnbeløp))
        val overNedreTerskel =
            Opplysningstype<Boolean>("Over nedre terskel", utledesAv = mutableSetOf(nedreTerskel, inntekt))
        val overØvreTerskel =
            Opplysningstype<Boolean>("Over øvre terskel", utledesAv = mutableSetOf(øvreTerskel, inntekt))

        // val minsteinntekt = Opplysningstype("Minsteinntekt", vilkår, utledesAv = mutableSetOf(overNedreTerskel, overØvreTerskel))
        val minsteinntekt =
            Opplysningstype("Minsteinntekt", vilkår, regel = EnAvRegel(overNedreTerskel, overØvreTerskel))

        val opplysninger = Opplysninger()

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

        opplysninger.leggTil(Faktum(grunnbeløp, 123123.0))
        assertEquals(0, opplysninger.trenger(minsteinntekt).size)

        assertTrue(opplysninger.har(minsteinntekt))
        assertTrue(opplysninger.find { it.er(minsteinntekt) }?.verdi as Boolean)
    }
}
