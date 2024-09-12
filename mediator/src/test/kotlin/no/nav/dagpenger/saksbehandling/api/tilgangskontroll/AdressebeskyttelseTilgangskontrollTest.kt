package no.nav.dagpenger.saksbehandling.api.tilgangskontroll

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.UUIDv7
import org.junit.jupiter.api.Test

class AdressebeskyttelseTilgangskontrollTest {
    private val fortroligGruppe = "Fortrolig"
    private val strengtFortroligGruppe = "StrengtFortrolig"
    private val strengtFortroligUtlandGruppe = "StrengtFortroligUtland"

    @Test
    fun `Saksbehandler har alltid tilgang dersom oppgaven tilhører en person som ikke er adressebeskyttet`() {
        AdressebeskyttelseTilgangskontroll(
            strengtFortroligGruppe = strengtFortroligGruppe,
            strengtFortroligUtlandGruppe = strengtFortroligUtlandGruppe,
            fortroligGruppe = fortroligGruppe,
            adressebeskyttelseGraderingFun = { AdressebeskyttelseGradering.UGRADERT },
        ).let { tilgangskontroll ->
            tilgangskontroll.harTilgang(UUIDv7.ny(), Saksbehandler("ident", emptySet(), "token")) shouldBe true
            tilgangskontroll.harTilgang(UUIDv7.ny(), Saksbehandler("ident", setOf(fortroligGruppe), "token")) shouldBe true
            tilgangskontroll.harTilgang(
                UUIDv7.ny(),
                Saksbehandler("ident", setOf(strengtFortroligGruppe), "token"),
            ) shouldBe true
            tilgangskontroll.harTilgang(
                UUIDv7.ny(),
                Saksbehandler("ident", setOf(strengtFortroligUtlandGruppe), "token"),
            ) shouldBe true
        }
    }

    @Test
    fun `Saksbehandler har ikke tilgang dersom hen ikke har fortrolig`() {
        AdressebeskyttelseTilgangskontroll(
            strengtFortroligGruppe = strengtFortroligGruppe,
            strengtFortroligUtlandGruppe = strengtFortroligUtlandGruppe,
            fortroligGruppe = fortroligGruppe,
            adressebeskyttelseGraderingFun = { AdressebeskyttelseGradering.FORTROLIG },
        ).let { tilgangskontroll ->
            tilgangskontroll.harTilgang(UUIDv7.ny(), Saksbehandler("ident", emptySet(), "token")) shouldBe false
            tilgangskontroll.harTilgang(UUIDv7.ny(), Saksbehandler("ident", setOf(fortroligGruppe), "token")) shouldBe true
            tilgangskontroll.harTilgang(
                UUIDv7.ny(),
                Saksbehandler("ident", setOf(strengtFortroligGruppe), "token"),
            ) shouldBe false
            tilgangskontroll.harTilgang(
                UUIDv7.ny(),
                Saksbehandler("ident", setOf(strengtFortroligUtlandGruppe), "token"),
            ) shouldBe false
        }
    }

    @Test
    fun `Saksbehandler har ikke tilgang dersom hen ikke har strengtFortrolig`() {
        AdressebeskyttelseTilgangskontroll(
            strengtFortroligGruppe = strengtFortroligGruppe,
            strengtFortroligUtlandGruppe = strengtFortroligUtlandGruppe,
            fortroligGruppe = fortroligGruppe,
            adressebeskyttelseGraderingFun = { AdressebeskyttelseGradering.STRENGT_FORTROLIG },
        ).let { tilgangskontroll ->
            tilgangskontroll.harTilgang(UUIDv7.ny(), Saksbehandler("ident", emptySet(), "token")) shouldBe false
            tilgangskontroll.harTilgang(UUIDv7.ny(), Saksbehandler("ident", setOf(fortroligGruppe), "token")) shouldBe false
            tilgangskontroll.harTilgang(
                UUIDv7.ny(),
                Saksbehandler("ident", setOf(strengtFortroligGruppe), "token"),
            ) shouldBe true
            tilgangskontroll.harTilgang(
                UUIDv7.ny(),
                Saksbehandler("ident", setOf(strengtFortroligUtlandGruppe), "token"),
            ) shouldBe false
        }
    }

    @Test
    fun `Saksbehandler har ikke tilgang dersom hen ikke har strengtFortroligUtland`() {
        AdressebeskyttelseTilgangskontroll(
            strengtFortroligGruppe = strengtFortroligGruppe,
            strengtFortroligUtlandGruppe = strengtFortroligUtlandGruppe,
            fortroligGruppe = fortroligGruppe,
            adressebeskyttelseGraderingFun = { AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND },
        ).let { tilgangskontroll ->
            tilgangskontroll.harTilgang(UUIDv7.ny(), Saksbehandler("ident", emptySet(), "token")) shouldBe false
            tilgangskontroll.harTilgang(UUIDv7.ny(), Saksbehandler("ident", setOf(fortroligGruppe), "token")) shouldBe false
            tilgangskontroll.harTilgang(
                UUIDv7.ny(),
                Saksbehandler("ident", setOf(strengtFortroligGruppe), "token"),
            ) shouldBe false
            tilgangskontroll.harTilgang(
                UUIDv7.ny(),
                Saksbehandler("ident", setOf(strengtFortroligUtlandGruppe), "token"),
            ) shouldBe true
        }
    }

    @Test
    fun `Skrive ut feil type`() {
        AdressebeskyttelseTilgangskontroll(
            strengtFortroligGruppe = strengtFortroligGruppe,
            strengtFortroligUtlandGruppe = strengtFortroligUtlandGruppe,
            fortroligGruppe = fortroligGruppe,
            adressebeskyttelseGraderingFun = { AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND },
        ).let { tilgangskontroll ->
            tilgangskontroll.feilType(
                UUIDv7.ny(),
                Saksbehandler("ident", emptySet(), "token"),
            ) shouldBe "strengt-fortrolig-utland"
        }
    }
}
