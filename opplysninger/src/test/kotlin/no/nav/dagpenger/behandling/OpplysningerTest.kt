package no.nav.dagpenger.behandling

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class OpplysningerTest {
    @Test
    fun `vilkår er avhengig av andre vilkår`() {
        val vilkår = Opplysningstype("Vilkår")
        val minsteinntekt = Opplysningstype("Minsteinntekt", vilkår)
        val alder = Opplysningstype("Alder", vilkår)
        val fødselsdato = Opplysningstype("Fødselsdato")

        val opplysninger = Opplysninger()
        opplysninger.leggTil(Etterlyst(vilkår))

        assertEquals(3, opplysninger.size)
        assertTrue(opplysninger.har(minsteinntekt))
        assertTrue(opplysninger.har(alder))
    }
}
