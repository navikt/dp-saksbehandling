package no.nav.dagpenger.behandling.regel

import no.nav.dagpenger.behandling.Faktum
import no.nav.dagpenger.behandling.Opplysningstype
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StørreEnnTest {
    private val regel =
        StørreEnn(
            Opplysningstype<Boolean>("Vilkår"),
            Opplysningstype<Double>("A"),
            Opplysningstype<Double>("B"),
        )

    @Test
    fun `større enn`() {
        val utledet =
            regel.lagProdukt(
                listOf(
                    Faktum(Opplysningstype<Double>("A"), 2.0),
                    Faktum(Opplysningstype<Double>("B"), 1.0),
                ),
            )

        assertTrue(utledet.verdi)
    }

    @Test
    fun `ikke større enn`() {
        val utledet =
            regel.lagProdukt(
                listOf(
                    Faktum(Opplysningstype<Double>("A"), 1.0),
                    Faktum(Opplysningstype<Double>("B"), 2.0),
                ),
            )

        assertFalse(utledet.verdi)
    }

    @Test
    fun `mangler enn`() {
        val utledet =
            regel.lagProdukt(
                listOf(
                    Faktum(Opplysningstype<Double>("A"), 1.0),
                    Faktum(Opplysningstype<Double>("B"), 2.0),
                ),
            )

        assertFalse(utledet.verdi)
    }
}
