package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.regel.Regel
import no.nav.dagpenger.behandling.regel.enAvRegel
import no.nav.dagpenger.behandling.regel.multiplikasjon
import no.nav.dagpenger.behandling.regel.oppslag
import no.nav.dagpenger.behandling.regel.størreEnn
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import kotlin.test.assertEquals

class OpplysningerTest {
    @Test
    fun `vilkår er avhengig av andre vilkår`() {
        val vilkår = Opplysningstype<Boolean>("Vilkår")
        val minsteinntekt = Opplysningstype("Minsteinntekt", vilkår)
        val alder = Opplysningstype("Alder", vilkår)

        val opplysninger = Opplysninger(Regelkjøring(1.mai.atStartOfDay()))

        opplysninger.leggTil(Faktum(minsteinntekt, true))
        opplysninger.leggTil(Faktum(alder, true))
        opplysninger.leggTil(Faktum(vilkår, true))

        assertTrue(opplysninger.har(minsteinntekt))
        assertTrue(opplysninger.har(alder))
        assertTrue(opplysninger.har(vilkår))
    }

    @Test
    fun `tillater ikke overlappende opplysninger av samme type`() {
        val opplysningstype = Opplysningstype<Double>("Type")
        val opplysninger = Opplysninger(Regelkjøring(1.mai))

        opplysninger.leggTil(Faktum(opplysningstype, 0.5, Gyldighetsperiode(1.mai, 10.mai)))
        assertThrows<IllegalArgumentException> {
            opplysninger.leggTil(Faktum(opplysningstype, 0.5))
        }
        opplysninger.leggTil(Faktum(opplysningstype, 1.5, Gyldighetsperiode(11.mai)))

        assertEquals(0.5, opplysninger.finnOpplysning(opplysningstype).verdi)
        assertEquals(0.5, opplysninger.finnOpplysning(opplysningstype).verdi)
        // TODO: Denne testen er ikk elengre mulig nå som regelkjøring eier fraDato
        // assertEquals(1.5, opplysninger.finnOpplysning(opplysningstype).verdi)
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

        val fraDato = 10.mai.atStartOfDay()
        val opplysninger =
            Opplysninger(
                Regelkjøring(
                    fraDato,
                    regelsett,
                ),
                listOf(
                    // Setter opp opplysninger med ting som er kjent fra før
                    // Har er ikke lengre gyldig og må hentes på nytt
                    Hypotese(
                        inntekt,
                        321321.0,
                        Gyldighetsperiode(1.januar, 1.mai),
                    ),
                ),
            )

        // Sett virkningsdato som en opplysning
        opplysninger.leggTil(Faktum(virkningsdato, fraDato.toLocalDate()))
        val dag = DAG(opplysninger.regelkjøring.muligeRegler, minsteinntekt)
        println(dag.lag())
        // Flyt for å innhente manglende opplysninger
        val actual = opplysninger.trenger(minsteinntekt)
        assertEquals(3, actual.size)
        assertEquals(setOf(inntekt, nedreTerskelFaktor, øvreTerskelFaktor), actual)

        assertEquals(Grunnbeløp.TEST_GRUNNBELØP, opplysninger.finnOpplysning(grunnbeløp).verdi)

        opplysninger.leggTil(Faktum(nedreTerskelFaktor, 1.5))
        assertEquals(2, opplysninger.trenger(minsteinntekt).size)

        opplysninger.leggTil(Faktum(øvreTerskelFaktor, 3.0))
        assertEquals(1, opplysninger.trenger(minsteinntekt).size)

        // Har er ikke lengre gyldig inntekt og må hentes på nytt
        opplysninger.leggTil(Hypotese(inntekt, 321321.0, Gyldighetsperiode(9.mai)))
        assertEquals(0, opplysninger.trenger(minsteinntekt).size)

        assertTrue(opplysninger.har(minsteinntekt))
        assertTrue(opplysninger.finnOpplysning(minsteinntekt).verdi)

        println(opplysninger.toString())
    }
}

/*
1. Gyldighetsperiode for regler
2. Sporing av utledning
 */

class OpplysningNode(val navn: String, val produsertAv: RegelNode? = null)

class RegelNode(val navn: String, val avhengerAv: List<OpplysningNode>)

class DAG(val regler: List<Regel<*>>, val opplysningstype: Opplysningstype<*>) {
    fun lag(): OpplysningNode? {
        return regler.find { it.produserer(opplysningstype) }?.let {
            blurp(it)
        }
    }

    fun blurp(regel: Regel<*>): OpplysningNode {
        if (regel.avhengerAv.isEmpty()) {
            return OpplysningNode("Tom node")
        }
        val produserAvRegel =
            RegelNode(
                regel.toString(),
                // Alle noder regelen er avhengig av
                regel.avhengerAv.mapNotNull { opplysningstype ->
                    regler.find { it.produserer(opplysningstype) }
                        ?.let {
                            blurp(it)
                        }
                },
            )
        return OpplysningNode(
            regel.produserer.toString(),
            produserAvRegel,
        )
    }
}
