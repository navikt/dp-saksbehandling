package no.nav.dagpenger.saksbehandling.db

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.FORTROLIG
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.STRENGT_FORTROLIG
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.BehandlingType
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.TilgangType
import no.nav.dagpenger.saksbehandling.db.oppgave.Periode
import no.nav.dagpenger.saksbehandling.db.oppgave.TildelNesteOppgaveFilter
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TildelNesteOppgaveFilterTest {
    private val queryString =
        """emneknagg=knagg1&emneknagg=knagg2
        &fom=2021-01-01&tom=2023-01-01
        &tilstand=KLAR_TIL_KONTROLL&tilstand=UNDER_KONTROLL
        &behandlingType=KLAGE
        """.trimMargin()

    @Test
    fun `Skal kunne initialisere et søkefilter fra en url streng`() {
        val saksbehandler =
            Saksbehandler(
                navIdent = "saksbehandler",
                grupper = setOf(),
                tilganger = setOf(TilgangType.SAKSBEHANDLER),
            )
        TildelNesteOppgaveFilter.fra(
            queryString = queryString,
            saksbehandler = saksbehandler,
        ) shouldBe
            TildelNesteOppgaveFilter(
                periode =
                    Periode(
                        fom = LocalDate.of(2021, 1, 1),
                        tom = LocalDate.of(2023, 1, 1),
                    ),
                tilstander = setOf(Type.KLAR_TIL_KONTROLL, Type.UNDER_KONTROLL),
                behandlingTyper = setOf(BehandlingType.KLAGE),
                emneknagger = setOf("knagg1", "knagg2"),
                egneAnsatteTilgang = false,
                adressebeskyttelseTilganger = setOf(UGRADERT),
                navIdent = saksbehandler.navIdent,
            )
    }

    @Test
    fun `Skal sette adressebeskyttelse-tilganger korrekt på filter`() {
        TildelNesteOppgaveFilter.fra(
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
        TildelNesteOppgaveFilter.fra(
            queryString = "",
            saksbehandler = saksbehandler,
        ) shouldBe
            TildelNesteOppgaveFilter(
                periode = Periode.UBEGRENSET_PERIODE,
                emneknagger = setOf(),
                egneAnsatteTilgang = false,
                adressebeskyttelseTilganger = setOf(UGRADERT),
                navIdent = saksbehandler.navIdent,
            )
    }
}
