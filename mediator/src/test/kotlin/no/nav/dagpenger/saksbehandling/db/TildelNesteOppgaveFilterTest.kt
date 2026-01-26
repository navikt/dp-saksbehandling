package no.nav.dagpenger.saksbehandling.db

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.FORTROLIG
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.STRENGT_FORTROLIG
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.TilgangType
import no.nav.dagpenger.saksbehandling.UtløstAvType
import no.nav.dagpenger.saksbehandling.db.oppgave.Periode
import no.nav.dagpenger.saksbehandling.db.oppgave.TildelNesteOppgaveFilter
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TildelNesteOppgaveFilterTest {
    private val queryString =
        """emneknagg=knagg1&emneknagg=knagg2
        &fom=2021-01-01&tom=2023-01-01
        &tilstand=KLAR_TIL_KONTROLL&tilstand=UNDER_KONTROLL
        &utlostAv=KLAGE
        """.trimMargin()

    @Test
    fun `Skal kunne initialisere et søkefilter fra en url streng`() {
        val saksbehandler =
            Saksbehandler(
                navIdent = "saksbehandler",
                grupper = setOf(),
                tilganger = setOf(TilgangType.SAKSBEHANDLER),
            )
        val filter =
            TildelNesteOppgaveFilter.fra(
                queryString = queryString,
                saksbehandler = saksbehandler,
            )
        filter.periode shouldBe
            Periode(
                fom = LocalDate.of(2021, 1, 1),
                tom = LocalDate.of(2023, 1, 1),
            )
        filter.tilstander shouldBe setOf(Type.KLAR_TIL_KONTROLL, Type.UNDER_KONTROLL)
        filter.utløstAvTyper shouldBe setOf(UtløstAvType.KLAGE)
        filter.emneknagger shouldBe setOf("knagg1", "knagg2")
        filter.egneAnsatteTilgang shouldBe false
        filter.adressebeskyttelseTilganger shouldBe setOf(UGRADERT)
        filter.navIdent shouldBe saksbehandler.navIdent
        filter.emneknaggGruppertPerKategori shouldBe mapOf("UDEFINERT" to setOf("knagg1", "knagg2"))
    }

    @Test
    fun `Skal sette adressebeskyttelse-tilganger korrekt på filter`() {
        TildelNesteOppgaveFilter
            .fra(
                queryString,
                saksbehandler =
                    Saksbehandler(
                        navIdent = "saksbehandler",
                        grupper = setOf(),
                        tilganger =
                            setOf(
                                TilgangType.SAKSBEHANDLER,
                                TilgangType.EGNE_ANSATTE,
                                TilgangType.FORTROLIG_ADRESSE,
                                TilgangType.STRENGT_FORTROLIG_ADRESSE,
                                TilgangType.STRENGT_FORTROLIG_ADRESSE_UTLAND,
                            ),
                    ),
            ).let { filter ->
                filter.egneAnsatteTilgang shouldBe true
                filter.adressebeskyttelseTilganger shouldBe
                    setOf(
                        UGRADERT,
                        FORTROLIG,
                        STRENGT_FORTROLIG,
                        STRENGT_FORTROLIG_UTLAND,
                    )
            }
    }

    @Test
    fun `Skal håndtere tom streng`() {
        val saksbehandler =
            Saksbehandler(
                navIdent = "saksbehandler",
                grupper = setOf(),
                tilganger = setOf(TilgangType.SAKSBEHANDLER),
            )
        val filter =
            TildelNesteOppgaveFilter.fra(
                queryString = "",
                saksbehandler = saksbehandler,
            )
        filter.periode shouldBe Periode.UBEGRENSET_PERIODE
        filter.emneknagger shouldBe setOf()
        filter.egneAnsatteTilgang shouldBe false
        filter.adressebeskyttelseTilganger shouldBe setOf(UGRADERT)
        filter.navIdent shouldBe saksbehandler.navIdent
        filter.emneknaggGruppertPerKategori shouldBe emptyMap()
    }
}
