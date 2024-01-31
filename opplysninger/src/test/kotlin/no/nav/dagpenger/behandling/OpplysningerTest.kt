package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.regel.enAvRegel
import no.nav.dagpenger.behandling.regel.multiplikasjon
import no.nav.dagpenger.behandling.regel.oppslag
import no.nav.dagpenger.behandling.regel.størreEnn
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime
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

        assertTrue(opplysninger.har(minsteinntekt, LocalDateTime.now()))
        assertTrue(opplysninger.har(alder, LocalDateTime.now()))
        assertTrue(opplysninger.har(vilkår, LocalDateTime.now()))
    }

    @Test
    fun `tillater ikke overlappende opplysninger av samme type`() {
        val opplysningstype = Opplysningstype<Double>("Type")
        val opplysninger = Opplysninger(Regelmotor())

        opplysninger.leggTil(Faktum(opplysningstype, 0.5, Gyldighetsperiode(1.mai, 10.mai)))
        assertThrows<IllegalArgumentException> {
            opplysninger.leggTil(Faktum(opplysningstype, 0.5))
        }
        opplysninger.leggTil(Faktum(opplysningstype, 1.5, Gyldighetsperiode(11.mai)))

        assertEquals(0.5, opplysninger.finnOpplysning(opplysningstype, 8.mai).verdi)
        assertEquals(0.5, opplysninger.finnOpplysning(opplysningstype, 10.mai).verdi)
        assertEquals(1.5, opplysninger.finnOpplysning(opplysningstype, 12.mai).verdi)
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

        val fraDato = LocalDateTime.now()
        val opplysninger = Opplysninger(Regelmotor(regelsett))
        opplysninger.leggTil(Faktum(virkningsdato, LocalDate.now()))
        val actual = opplysninger.trenger(minsteinntekt, fraDato)

        assertEquals(3, actual.size)
        assertEquals(setOf(inntekt, nedreTerskelFaktor, øvreTerskelFaktor), actual)

        assertEquals(Grunnbeløp.TEST_GRUNNBELØP, opplysninger.finnOpplysning(grunnbeløp).verdi)

        opplysninger.leggTil(Faktum(nedreTerskelFaktor, 1.5))
        assertEquals(2, opplysninger.trenger(minsteinntekt, fraDato).size)

        opplysninger.leggTil(Faktum(øvreTerskelFaktor, 3.0))
        assertEquals(1, opplysninger.trenger(minsteinntekt, fraDato).size)

        opplysninger.leggTil(
            Hypotese(
                inntekt,
                321321.0,
                // Gyldighetsperiode(LocalDate.now().minusYears(2), LocalDate.now().minusWeeks(2)),
            ),
        )
        assertEquals(0, opplysninger.trenger(minsteinntekt, fraDato).size)

        assertTrue(opplysninger.har(minsteinntekt, fraDato))
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
