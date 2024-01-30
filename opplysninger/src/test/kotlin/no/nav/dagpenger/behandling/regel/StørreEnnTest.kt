package no.nav.dagpenger.behandling.regel

import no.nav.dagpenger.behandling.Faktum
import no.nav.dagpenger.behandling.Opplysningstype
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StørreEnnTest {
    @Test
    fun `størreEnn regel`() {
        val regel =
            StørreEnn(
                Opplysningstype<Boolean>("Vilkår"),
                Opplysningstype<Double>("A"),
                Opplysningstype<Double>("B"),
            )

        val utledet =
            regel.lagProdukt(
                listOf(
                    Faktum(Opplysningstype<Double>("A"), 2.0),
                    Faktum(Opplysningstype<Double>("B"), 1.0),
                ),
            )

        assertTrue(utledet.verdi)
    }
}
