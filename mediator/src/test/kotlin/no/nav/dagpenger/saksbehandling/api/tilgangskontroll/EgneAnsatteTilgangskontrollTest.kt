package no.nav.dagpenger.saksbehandling.api.tilgangskontroll

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.UUIDv7
import org.junit.jupiter.api.Test

class EgneAnsatteTilgangskontrollTest {
    @Test
    fun `Saksbehandler har alltid tilgang dersom oppgaven tilhører en person som ikke er skjermet`() {
        EgneAnsatteTilgangskontroll(
            tillatteGrupper = setOf("A", "B"),
            skjermesSomEgneAnsatteFun = { false },
        ).let {
            it.harTilgang(UUIDv7.ny(), Saksbehandler("ident", emptySet())) shouldBe true
            it.harTilgang(UUIDv7.ny(), Saksbehandler("ident", setOf("C"))) shouldBe true
            it.harTilgang(UUIDv7.ny(), Saksbehandler("ident", setOf("A"))) shouldBe true
        }
    }

    @Test
    fun `Saksbehandler har tilgang dersom person er skjermet og saksbehandler er i en gruppe som har tilgang`() {
        EgneAnsatteTilgangskontroll(
            tillatteGrupper = setOf("A", "B"),
            skjermesSomEgneAnsatteFun = { true },
        ).let {
            it.harTilgang(UUIDv7.ny(), Saksbehandler("ident", setOf("B"))) shouldBe true
            it.harTilgang(UUIDv7.ny(), Saksbehandler("ident", setOf("A", "B"))) shouldBe true
            it.harTilgang(UUIDv7.ny(), Saksbehandler("ident", setOf("A", "B", "C"))) shouldBe true
        }
    }

    @Test
    fun `Saksbehandler har IKKE tilgang dersom person er skjermet og saksbehandler IKKE er i en gruppe som har tilgang`() {
        EgneAnsatteTilgangskontroll(
            tillatteGrupper = setOf("A", "B"),
            skjermesSomEgneAnsatteFun = { true },
        ).harTilgang(UUIDv7.ny(), Saksbehandler("ident", setOf("C"))) shouldBe false
    }

    @Test
    fun `Skrive ut feil type`() {
        EgneAnsatteTilgangskontroll(
            tillatteGrupper = setOf("A", "B"),
            skjermesSomEgneAnsatteFun = { true },
        ).let { tilgangskontroll ->
            tilgangskontroll.feilType(
                UUIDv7.ny(),
                Saksbehandler("ident", emptySet()),
            ) shouldBe "egne-ansatte"
        }
    }
}
