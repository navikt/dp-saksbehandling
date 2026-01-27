package no.nav.dagpenger.saksbehandling.db.oppgave

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.EmneknaggKategori
import org.junit.jupiter.api.Test

class FilterBuilderTest {
    @Test
    fun `Parser emneknagg grupper riktig`() {
        FilterBuilder("rettighet=A+B").emneknaggGruppertPerKategori() shouldBe
            mapOf(
                EmneknaggKategori.RETTIGHET to
                    setOf(
                        "A B",
                    ),
            )
    }
}
