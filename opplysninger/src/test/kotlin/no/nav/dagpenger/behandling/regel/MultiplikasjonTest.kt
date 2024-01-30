package no.nav.dagpenger.behandling.regel

import no.nav.dagpenger.behandling.Faktum
import no.nav.dagpenger.behandling.Opplysningstype
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MultiplikasjonTest {
    private val regel =
        Multiplikasjon(
            Opplysningstype("SUM"),
            Opplysningstype("A"),
            Opplysningstype("B"),
        )

    @Test
    fun `multiplikasjon regel`() {
        val utledet =
            regel.lagProdukt(
                listOf(
                    Faktum(Opplysningstype("A"), 2.0),
                    Faktum(Opplysningstype("B"), 2.0),
                ),
            )

        assertEquals(4.0, utledet.verdi)
    }
}
