package no.nav.dagpenger.behandling

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OpplysningTest {
    @Test
    fun `Har opplysningstype`() {
        val opplysning = Faktum(Opplysningstype<LocalDate>("Fødselsdato"), LocalDate.now())
        assertTrue(opplysning.er(Opplysningstype<LocalDate>("Fødselsdato")))
    }
}
