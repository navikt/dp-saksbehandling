package no.nav.dagpenger.saksbehandling.db

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.AdresseBeskyttelseGradering
import no.nav.dagpenger.saksbehandling.db.oppgave.Periode
import no.nav.dagpenger.saksbehandling.db.oppgave.TildelNesteOppgaveFilter
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TildelNesteOppgaveFilterTest {
    @Test
    fun `Skal kunne initialisere et søkefilter fra en url streng`() {
        val queryString =
            """emneknagg=knagg1&emneknagg=knagg2&fom=2021-01-01&tom=2023-01-01"""
        val saksbehandlerTilgangEgneAnsatte = false
        TildelNesteOppgaveFilter.fra(queryString, saksbehandlerTilgangEgneAnsatte) shouldBe
            TildelNesteOppgaveFilter(
                periode =
                    Periode(
                        fom = LocalDate.of(2021, 1, 1),
                        tom = LocalDate.of(2023, 1, 1),
                    ),
                emneknagg = setOf("knagg1", "knagg2"),
                harTilgangTilEgneAnsatte = saksbehandlerTilgangEgneAnsatte,
                harTilgangTilAdressebeskyttede = AdresseBeskyttelseGradering.FORTROLIG,
            )
    }

    @Test
    fun `Skal håndtere tom streng`() {
        val queryString = ""
        val saksbehandlerTilgangEgneAnsatte = false
        TildelNesteOppgaveFilter.fra(queryString, saksbehandlerTilgangEgneAnsatte) shouldBe
            TildelNesteOppgaveFilter(
                periode = Periode.UBEGRENSET_PERIODE,
                emneknagg = setOf(),
                harTilgangTilEgneAnsatte = saksbehandlerTilgangEgneAnsatte,
                harTilgangTilAdressebeskyttede = AdresseBeskyttelseGradering.FORTROLIG,
            )
    }
}
