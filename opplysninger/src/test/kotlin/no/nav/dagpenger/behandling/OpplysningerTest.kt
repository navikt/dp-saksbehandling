package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.regel.enAvRegel
import no.nav.dagpenger.behandling.regel.multiplikasjon
import no.nav.dagpenger.behandling.regel.oppslag
import no.nav.dagpenger.behandling.regel.størreEnn
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
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

        assertTrue(opplysninger.har(minsteinntekt))
        assertTrue(opplysninger.har(alder))
        assertTrue(opplysninger.har(vilkår))
    }

    private object Grunnbeløp {
        const val TEST_GRUNNBELØP = 118620.0

        fun finnFor(dato: LocalDate) = TEST_GRUNNBELØP
    }

    @Test
    fun `finn alle løvnoder som mangler`() {
        val vilkår = Opplysningstype<Boolean>("Vilkår")
        val regelsett = Regelsett()

        val nedreTerskelFaktor = Opplysningstype<Double>("Nedre terskel (1.5G)")
        val øvreTerskelFaktor = Opplysningstype<Double>("Øvre terskel (1.5G)")
        val inntekt = Opplysningstype<Double>("Inntekt")
        val grunnbeløp = Opplysningstype<Double>("Grunnbeløp")
        val virkningsdato = Opplysningstype<LocalDate>("Virkningsdato")

        regelsett.oppslag(grunnbeløp, virkningsdato) { Grunnbeløp.finnFor(it) }

        val nedreTerskel = Opplysningstype<Double>("Inntektskrav for nedre terskel (1.5G)")
        regelsett.multiplikasjon(nedreTerskel, nedreTerskelFaktor, grunnbeløp)

        val øvreTerskel = Opplysningstype<Double>("Inntektskrav for øvre terskel (3G)")
        regelsett.multiplikasjon(øvreTerskel, øvreTerskelFaktor, grunnbeløp)

        val overNedreTerskel = Opplysningstype<Boolean>("Inntekt er over nedre terskel (1.5G)")
        regelsett.størreEnn(overNedreTerskel, inntekt, nedreTerskel)

        val overØvreTerskel = Opplysningstype<Boolean>("Inntekt er over øvre terskel (3G)")
        regelsett.størreEnn(overØvreTerskel, inntekt, øvreTerskel)

        val minsteinntekt = Opplysningstype("Minsteinntekt", vilkår)
        regelsett.enAvRegel(minsteinntekt, overNedreTerskel, overØvreTerskel)

        val opplysninger = Opplysninger(Regelmotor(regelsett))
        opplysninger.leggTil(Faktum(virkningsdato, LocalDate.now()))
        val actual = opplysninger.trenger(minsteinntekt)

        assertEquals(3, actual.size)
        assertEquals(setOf(inntekt, nedreTerskelFaktor, øvreTerskelFaktor), actual)

        assertEquals(Grunnbeløp.TEST_GRUNNBELØP, opplysninger.finnOpplysning(grunnbeløp).verdi)

        opplysninger.leggTil(Faktum(nedreTerskelFaktor, 1.5))
        assertEquals(2, opplysninger.trenger(minsteinntekt).size)

        opplysninger.leggTil(Faktum(øvreTerskelFaktor, 3.0))
        assertEquals(1, opplysninger.trenger(minsteinntekt).size)

        opplysninger.leggTil(Hypotese(inntekt, 321321.0))
        assertEquals(0, opplysninger.trenger(minsteinntekt).size)

        assertTrue(opplysninger.har(minsteinntekt))
        assertTrue(opplysninger.finnOpplysning(minsteinntekt).verdi)

        println(opplysninger.toString())
    }
}

/*
1. Gyldighetsperiode for opplysninger
2. Gyldighetsperiode for regler
3. Behandling med utgangspunkt i en dato
4. Sporing av utledning
 */
