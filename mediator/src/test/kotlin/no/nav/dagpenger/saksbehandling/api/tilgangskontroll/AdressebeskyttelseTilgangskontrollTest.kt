package no.nav.dagpenger.saksbehandling.api.tilgangskontroll

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.AdresseBeskyttelseGradering
import no.nav.dagpenger.saksbehandling.UUIDv7
import org.junit.jupiter.api.Test

class AdressebeskyttelseTilgangskontrollTest {
    @Test
    fun `Saksbehandler har alltid tilgang dersom oppgaven tilhører en person som ikke er adressebeskyttet`() {
        val fortroligGruppe = "Fortrolig"
        val strengtFortroligGruppe = "StrengtFortrolig"
        AdressebeskyttelseTilgangskontroll(
            fortroligGruppe = fortroligGruppe,
            strengtFortroligGruppe = strengtFortroligGruppe,
            adressebeskyttelseGraderingFun = { AdresseBeskyttelseGradering.UGRADERT },
        ).let { tilgangskontroll ->
            tilgangskontroll.harTilgang(UUIDv7.ny(), Saksbehandler("ident", emptySet())) shouldBe true
            tilgangskontroll.harTilgang(UUIDv7.ny(), Saksbehandler("ident", setOf(fortroligGruppe))) shouldBe true
            tilgangskontroll.harTilgang(UUIDv7.ny(), Saksbehandler("ident", setOf(strengtFortroligGruppe))) shouldBe true
        }
    }

/*
    @Test
    fun `Saksbehandler har tilgang dersom person er skjermet og saksbehandler er i en gruppe som har tilgang`() {
        AdressebeskyttelseTilgangskontroll(
            tillatteGrupper = setOf("A", "B"),
            skjermesSomEgneAnsatteFun = { PDLPerson.AdressebeskyttelseGradering.STRENGT_FORTROLIG },
        ).let {
            it.harTilgang(UUIDv7.ny(), Saksbehandler("ident", setOf("B"))) shouldBe true
            it.harTilgang(UUIDv7.ny(), Saksbehandler("ident", setOf("A", "B"))) shouldBe true
            it.harTilgang(UUIDv7.ny(), Saksbehandler("ident", setOf("A", "B", "C"))) shouldBe true
        }
    }
*/

    @Test
    fun `Saksbehandler har IKKE tilgang dersom person er skjermet og saksbehandler IKKE er i en gruppe som har tilgang`() {
        EgneAnsatteTilgangskontroll(
            tillatteGrupper = setOf("A", "B"),
            skjermesSomEgneAnsatteFun = { true },
        ).harTilgang(UUIDv7.ny(), Saksbehandler("ident", setOf("C"))) shouldBe false
    }
}
