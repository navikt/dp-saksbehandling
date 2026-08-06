package no.nav.dagpenger.saksbehandling.db.oppgave

import io.kotest.assertions.throwables.shouldNotThrowAnyUnit
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.ktor.http.Parameters
import no.nav.dagpenger.saksbehandling.EmneknaggKategori
import no.nav.dagpenger.saksbehandling.HendelseBehandler
import no.nav.dagpenger.saksbehandling.Oppgave
import org.junit.jupiter.api.Test
import java.time.LocalDate

class SøkefilterTest {
    @Test
    fun `Skal kunne initialisere et søkefilter fra Ktors QueryParameters`() {
        Parameters
            .build {
                this.appendAll("tilstand", listOf("KLAR_TIL_BEHANDLING", "UNDER_BEHANDLING"))
                this.appendAll("ekskluderEmneknagg", listOf("D-nummer", "Planlegger utdanning"))
                this.appendAll("utlostAv", listOf("SØKNAD", "KLAGE"))
                this.appendAll("rettighet", listOf("Permittert", "Permittert fisk"))
                this["sorteringsfelt"] = "status"
                this["sortering"] = "asc"
                this["fom"] = "2021-01-01"
                this["tom"] = "2023-01-01"
                this["mineOppgaver"] = "true"
                this["harDpSak"] = "true"
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
                        HendelseBehandler.DpBehandling.Søknad,
                        HendelseBehandler.Intern.Klage,
                    )
                søkefilter.ekskluderEmneknagger shouldBe setOf("D-nummer", "Planlegger utdanning")
                søkefilter.emneknaggGruppertPerKategori shouldBe
                    mapOf(
                        EmneknaggKategori.RETTIGHET to
                            setOf(
                                "Permittert",
                                "Permittert fisk",
                            ),
                    )
                søkefilter.behandlerIdent shouldBe "testIdent"
                søkefilter.harDpSak shouldBe true
                søkefilter.paginering shouldBe Søkefilter.Paginering(10, 0)
                søkefilter.sorteringsfelt shouldBe Søkefilter.Sorteringsfelt.STATUS
                søkefilter.sortering shouldBe Søkefilter.Sortering.ASC
                søkefilter.emneknaggGruppertPerKategori.shouldBe(
                    mapOf(
                        EmneknaggKategori.RETTIGHET to setOf("Permittert", "Permittert fisk"),
                    ),
                )
            }
    }

    @Test
    fun `Bruk default verdier når query parameters mangler mine, tilstand, ekskluderEmneknagger, fom, tom eller paginering`() {
        val søkefilter = Søkefilter.fra(Parameters.Empty, "testIdent")
        søkefilter.periode shouldBe Periode.UBEGRENSET_PERIODE
        søkefilter.tilstander shouldBe Oppgave.Tilstand.Type.søkbareTilstander
        søkefilter.ekskluderEmneknagger shouldBe emptySet()
        søkefilter.harDpSak shouldBe false
        søkefilter.behandlerIdent shouldBe null
        søkefilter.personIdent shouldBe null
        søkefilter.oppgaveId shouldBe null
        søkefilter.behandlingId shouldBe null
        søkefilter.paginering shouldBe Søkefilter.Paginering(20, 0)
        søkefilter.emneknaggGruppertPerKategori shouldBe emptyMap()
        søkefilter.sorteringsfelt shouldBe Søkefilter.Sorteringsfelt.OPPRETTET
        søkefilter.sortering shouldBe Søkefilter.Sortering.ASC
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
    fun `Skal kunne bruke kategori-baserte query parametere`() {
        Parameters
            .build {
                this.appendAll("rettighet", listOf("Ordinær", "Verneplikt"))
                this.appendAll("soknadsresultat", listOf("Avslag"))
                this["fom"] = "2021-01-01"
                this["tom"] = "2023-01-01"
            }.let {
                val søkefilter = Søkefilter.fra(it, "testIdent")
                søkefilter.emneknaggGruppertPerKategori shouldBe
                    mapOf(
                        EmneknaggKategori.RETTIGHET to setOf("Ordinær", "Verneplikt"),
                        EmneknaggKategori.SØKNADSRESULTAT to setOf("Avslag"),
                    )
            }
    }

    @Test
    fun `Sortering default skal være ASC`() {
        val søkefilter = Søkefilter.fra(Parameters.Empty, "testIdent")
        søkefilter.sortering shouldBe Søkefilter.Sortering.ASC
    }

    @Test
    fun `Skal kunne sette sorteringsfelt til STATUS`() {
        Parameters
            .build {
                this["sorteringsfelt"] = "status"
            }.let {
                val søkefilter = Søkefilter.fra(it, "testIdent")
                søkefilter.sorteringsfelt shouldBe Søkefilter.Sorteringsfelt.STATUS
            }
    }

    @Test
    fun `Skal kunne sette sortering til ASC eksplisitt`() {
        Parameters
            .build {
                this["sortering"] = "asc"
            }.let {
                val søkefilter = Søkefilter.fra(it, "testIdent")
                søkefilter.sortering shouldBe Søkefilter.Sortering.ASC
            }
    }

    @Test
    fun `Skal kunne sette sortering til DESC eksplisitt`() {
        Parameters
            .build {
                this["sortering"] = "desc"
            }.let {
                val søkefilter = Søkefilter.fra(it, "testIdent")
                søkefilter.sortering shouldBe Søkefilter.Sortering.DESC
            }
    }

    @Test
    fun `Skal bruke default verdier for sorteringsfelt og sortering ved ugyldige verdier`() {
        Parameters
            .build {
                this["sorteringsfelt"] = "ukjent"
                this["sortering"] = "ukjent"
            }.let {
                val søkefilter = Søkefilter.fra(it, "testIdent")
                søkefilter.sorteringsfelt shouldBe Søkefilter.Sorteringsfelt.OPPRETTET
                søkefilter.sortering shouldBe Søkefilter.Sortering.ASC
            }
    }

    @Test
    fun `Skal kunne sette sorteringsfelt til UTSATT_TIL`() {
        Parameters
            .build {
                this["sorteringsfelt"] = "utsattTil"
            }.let {
                val søkefilter = Søkefilter.fra(it, "testIdent")
                søkefilter.sorteringsfelt shouldBe Søkefilter.Sorteringsfelt.UTSATT_TIL
            }
    }

    @Test
    fun `Skal kunne filtrere på en eksplisitt behandlerIdent`() {
        Parameters
            .build {
                this["saksbehandlerIdent"] = "annenIdent"
            }.let {
                val søkefilter = Søkefilter.fra(it, "testIdent")
                søkefilter.behandlerIdent shouldBe "annenIdent"
                søkefilter.utenBehandler shouldBe false
            }
    }

    @Test
    fun `Eksplisitt behandlerIdent skal vinne over mineOppgaver`() {
        Parameters
            .build {
                this["saksbehandlerIdent"] = "annenIdent"
                this["mineOppgaver"] = "true"
            }.let {
                val søkefilter = Søkefilter.fra(it, "testIdent")
                søkefilter.behandlerIdent shouldBe "annenIdent"
            }
    }

    @Test
    fun `Skal kunne filtrere på oppgaver uten saksbehandler`() {
        Parameters
            .build {
                this["utenSaksbehandler"] = "true"
            }.let {
                val søkefilter = Søkefilter.fra(it, "testIdent")
                søkefilter.utenBehandler shouldBe true
                søkefilter.behandlerIdent shouldBe null
            }
    }

    @Test
    fun `utenSaksbehandler skal vinne over mineOppgaver og eksplisitt saksbehandlerIdent`() {
        Parameters
            .build {
                this["utenSaksbehandler"] = "true"
                this["mineOppgaver"] = "true"
                this["saksbehandlerIdent"] = "annenIdent"
            }.let {
                val søkefilter = Søkefilter.fra(it, "testIdent")
                søkefilter.behandlerIdent shouldBe null
            }
    }
}
