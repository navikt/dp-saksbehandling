package no.nav.dagpenger.saksbehandling.db

import io.kotest.assertions.throwables.shouldNotThrowAnyUnit
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.ktor.http.Parameters
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SøkefilterTest {
    @Test
    fun `Skal kunne initialisere et søkefilter fra Ktor sin QueryParameters`() {
        Parameters.build {
            this.appendAll("tilstand", listOf("KLAR_TIL_BEHANDLING", "UNDER_BEHANDLING"))
            this["fom"] = "2021-01-01"
            this["tom"] = "2023-01-01"
            this["mineOppgaver"] = "true"
        }.let {
            Søkefilter.fra(it, "testIdent") shouldBe
                Søkefilter(
                    periode =
                        Periode(
                            fom = LocalDate.of(2021, 1, 1),
                            tom = LocalDate.of(2023, 1, 1),
                        ),
                    tilstand = setOf(KLAR_TIL_BEHANDLING, UNDER_BEHANDLING),
                    saksbehandlerIdent = "testIdent",
                )
        }
    }

    @Test
    fun `Bruk default verdier dersom query parameters ikke inneholder mine, tilstand, fom eller tom`() {
        Søkefilter.fra(Parameters.Empty, "testIdent") shouldBe
            Søkefilter(
                periode = Periode.UBEGRENSET_PERIODE,
                tilstand = Oppgave.Tilstand.Type.defaultOppgaveListTilstander,
                saksbehandlerIdent = null,
                personIdent = null,
                oppgaveId = null,
                behandlingId = null,
            )
    }

    @Test
    fun `Fom for en periode må være før eller lik tom`() {
        shouldThrow<IllegalArgumentException> {
            Periode(fom = LocalDate.MIN.plusDays(1), tom = LocalDate.MIN)
        }
        shouldNotThrowAnyUnit {
            Periode(fom = LocalDate.MIN, tom = LocalDate.MAX)
        }
        shouldNotThrowAnyUnit {
            Periode(fom = LocalDate.MIN, tom = LocalDate.MIN)
        }
    }
}
