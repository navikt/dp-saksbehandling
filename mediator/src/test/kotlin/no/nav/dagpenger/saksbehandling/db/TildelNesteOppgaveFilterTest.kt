package no.nav.dagpenger.saksbehandling.db

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TildelNesteOppgaveFilterTest {
    @Test
    fun `Skal kunne initialisere et søkefilter fra en url streng`() {
        val queryString =
            """emneknagg=knagg1&emneknagg=knagg2&fom=2021-01-01&tom=2023-01-01"""
        TildelNesteOppgaveFilter.fra(queryString) shouldBe
            TildelNesteOppgaveFilter(
                periode =
                    Periode(
                        fom = LocalDate.of(2021, 1, 1),
                        tom = LocalDate.of(2023, 1, 1),
                    ),
                emneknagg = setOf("knagg1", "knagg2"),
            )
    }

    @Test
    fun `Skal håndtere tom streng`() {
        val queryString = ""
        TildelNesteOppgaveFilter.fra(queryString) shouldBe
            TildelNesteOppgaveFilter(
                periode = Periode.UBEGRENSET_PERIODE,
                emneknagg = setOf(),
            )
    }
}
