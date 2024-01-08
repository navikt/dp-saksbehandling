package no.nav.dagpenger.behandling

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class OpplysningTest {
    @Test
    fun `Har opplysningstype`() {
        val opplysning = Faktum(Opplysningstype("Fødselsdato"))
        assertTrue(opplysning.er(Opplysningstype("Fødselsdato")))
    }
}
