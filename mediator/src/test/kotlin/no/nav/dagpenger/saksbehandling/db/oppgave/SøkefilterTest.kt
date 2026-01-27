package no.nav.dagpenger.saksbehandling.db.oppgave

import io.kotest.assertions.throwables.shouldNotThrowAnyUnit
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.ktor.http.Parameters
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.UtløstAvType
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SøkefilterTest {
    @Test
    fun `Skal kunne initialisere et søkefilter fra Ktor sin QueryParameters`() {
        Parameters.Companion
            .build {
                this.appendAll("tilstand", listOf("KLAR_TIL_BEHANDLING", "UNDER_BEHANDLING"))
                this.appendAll("utlostAv", listOf("SØKNAD", "KLAGE"))
                this.appendAll("emneknagg", listOf("Permittert", "Permittert fisk"))
                this["fom"] = "2021-01-01"
                this["tom"] = "2023-01-01"
                this["mineOppgaver"] = "true"
                this["antallOppgaver"] = "10"
                this["side"] = "1"
            }.let {
                val søkefilter = Søkefilter.fra(it, "testIdent")
                søkefilter.periode shouldBe
                    Periode(
                        fom = LocalDate.of(2021, 1, 1),
                        tom = LocalDate.of(2023, 1, 1),
                    )
                søkefilter.tilstander shouldBe
                    setOf(
                        Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING,
                        Oppgave.Tilstand.Type.UNDER_BEHANDLING,
                    )
                søkefilter.utløstAvTyper shouldBe
                    setOf(
                        UtløstAvType.SØKNAD,
                        UtløstAvType.KLAGE,
                    )
                søkefilter.emneknagger shouldBe setOf("Permittert", "Permittert fisk")
                søkefilter.saksbehandlerIdent shouldBe "testIdent"
                søkefilter.paginering shouldBe Søkefilter.Paginering(10, 0)
                søkefilter.emneknaggGruppertPerKategori.shouldBe(
                    mapOf(
                        "RETTIGHET" to setOf("Permittert", "Permittert fisk"),
                    ),
                )
            }
    }

    @Test
    fun `Bruk default verdier dersom query parameters ikke inneholder mine, tilstand, fom, tom eller paginering`() {
        val søkefilter = Søkefilter.fra(Parameters.Companion.Empty, "testIdent")
        søkefilter.periode shouldBe Periode.UBEGRENSET_PERIODE
        søkefilter.tilstander shouldBe Oppgave.Tilstand.Type.Companion.søkbareTilstander
        søkefilter.saksbehandlerIdent shouldBe null
        søkefilter.personIdent shouldBe null
        søkefilter.oppgaveId shouldBe null
        søkefilter.behandlingId shouldBe null
        søkefilter.paginering shouldBe Søkefilter.Paginering(20, 0)
        søkefilter.emneknaggGruppertPerKategori shouldBe emptyMap()
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

    @Test
    fun `Ugyldige verdier for paginering skal kaste feil`() {
        shouldThrow<IllegalArgumentException> {
            Søkefilter.Paginering(10, -1)
        }
        shouldThrow<IllegalArgumentException> {
            Søkefilter.Paginering(0, 2)
        }
    }

    @Test
    fun `Skal kunne bruke emneknaggKategori som query parameter`() {
        Parameters.Companion
            .build {
                this.appendAll("emneknaggKategori", listOf("Avslag", "Ordinær"))
                this["fom"] = "2021-01-01"
                this["tom"] = "2023-01-01"
            }.let {
                val søkefilter = Søkefilter.fra(it, "testIdent")
                søkefilter.emneknagger shouldBe setOf("Avslag", "Ordinær")
                søkefilter.emneknaggGruppertPerKategori shouldBe
                    mapOf(
                        "SØKNADSRESULTAT" to setOf("Avslag"),
                        "RETTIGHET" to setOf("Ordinær"),
                    )
            }
    }

    @Test
    fun `emneknaggKategori skal ha prioritet over emneknagg hvis begge er satt`() {
        Parameters.Companion
            .build {
                this.appendAll("emneknagg", listOf("Gammel", "Parameter"))
                this.appendAll("emneknaggKategori", listOf("Avslag", "Innvilgelse"))
            }.let {
                val søkefilter = Søkefilter.fra(it, "testIdent")
                // Skal bruke emneknaggKategori
                søkefilter.emneknagger shouldBe setOf("Avslag", "Innvilgelse")
            }
    }

    @Test
    fun `Skal kunne bruke kategori-baserte query parametere`() {
        Parameters.Companion
            .build {
                this.appendAll("rettighet", listOf("Ordinær", "Verneplikt"))
                this.appendAll("søknadsresultat", listOf("Avslag"))
                this["fom"] = "2021-01-01"
                this["tom"] = "2023-01-01"
            }.let {
                val søkefilter = Søkefilter.fra(it, "testIdent")
                søkefilter.emneknagger shouldBe setOf("Ordinær", "Verneplikt", "Avslag")
                søkefilter.emneknaggGruppertPerKategori shouldBe
                    mapOf(
                        "RETTIGHET" to setOf("Ordinær", "Verneplikt"),
                        "SØKNADSRESULTAT" to setOf("Avslag"),
                    )
            }
    }

    @Test
    fun `Kategori-baserte parametere skal ha prioritet over emneknagg og emneknaggKategori`() {
        Parameters.Companion
            .build {
                this.appendAll("emneknagg", listOf("Gammel1"))
                this.appendAll("emneknaggKategori", listOf("Gammel2"))
                this.appendAll("rettighet", listOf("Ordinær"))
            }.let {
                val søkefilter = Søkefilter.fra(it, "testIdent")
                // Skal bruke kategori-baserte parametere
                søkefilter.emneknagger shouldBe setOf("Ordinær")
                søkefilter.emneknaggGruppertPerKategori shouldBe
                    mapOf(
                        "RETTIGHET" to setOf("Ordinær"),
                    )
            }
    }
}
