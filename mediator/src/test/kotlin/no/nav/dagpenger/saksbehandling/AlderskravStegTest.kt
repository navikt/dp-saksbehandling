package no.nav.dagpenger.saksbehandling

import io.kotest.assertions.throwables.shouldThrow
import org.junit.jupiter.api.Test

class AlderskravStegTest {
    @Test
    fun `AlderkraveSteg må ha en toppnode med boolsk verdi`() {
        val opplysninger = listOf(
            Opplysning(
                navn = "Testvilkår",
                verdi = "true",
                dataType = "string",
                status = OpplysningStatus.Faktum,
            ),
        )

        shouldThrow<IllegalStateException> {
            AlderskravSteg("hubba", opplysninger)
        }
    }
}
