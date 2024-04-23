package no.nav.dagpenger.saksbehandling.db

import io.kotest.assertions.throwables.shouldNotThrowAnyUnit
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.ktor.http.Parameters
import no.nav.dagpenger.saksbehandling.Oppgave
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SøkefilterTest {

    @Test
    fun `Skal kunne initialisere et søkefilter fra Ktor sin QueryParameters`() {
        Parameters.build {
            this["tilstand"] = "KLAR_TIL_BEHANDLING"
            this["fom"] = "2021-01-01"
            this["tom"] = "2023-01-01"
            this["mineOppgaver"] = "true"
        }.let {
            Søkefilter.fra(it, "testIdent") shouldBe Søkefilter(
                periode = Søkefilter.Periode(
                    fom = LocalDate.of(2021, 1, 1),
                    tom = LocalDate.of(2023, 1, 1),
                ),
                tilstand = Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING,
                saksbehandlerIdent = "testIdent",
            )
        }
    }

    @Test
    fun `Bruk default verdier dersom query parameters ikke inneholder mine, tilstand, fom eller tom`() {
        Søkefilter.fra(Parameters.Empty, "testIdent") shouldBe Søkefilter.DEFAULT_SØKEFILTER
    }

    @Test
    fun `Fom for en periode må være før tom`() {
        shouldThrow<IllegalArgumentException> {
            Søkefilter.Periode(fom = LocalDate.MAX, tom = LocalDate.MIN)
        }

        shouldThrow<IllegalArgumentException> {
            Søkefilter.Periode(fom = LocalDate.MIN, tom = LocalDate.MIN)
        }

        shouldNotThrowAnyUnit {
            Søkefilter.Periode(fom = LocalDate.MIN, tom = LocalDate.MAX)
        }
    }
}
