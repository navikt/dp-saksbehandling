package no.nav.dagpenger.saksbehandling.tilgangskontroll

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.api.Saksbehandler
import org.junit.jupiter.api.Test

class EgenAnsattTilgangsKontrollTest {
    @Test
    fun `Saksbehandler har alltid tilgang dersom oppgaven tilhører en person som ikke er skjermet`() {
        EgenAnsattTilgangsKontroll(
            tillatteGrupper = setOf("A", "B"),
            erEgenAnssattFun = { false },
        ).let {
            it.harTilgang(UUIDv7.ny(), Saksbehandler("ident", emptySet())) shouldBe true
            it.harTilgang(UUIDv7.ny(), Saksbehandler("ident", setOf("C"))) shouldBe true
            it.harTilgang(UUIDv7.ny(), Saksbehandler("ident", setOf("A"))) shouldBe true
        }
    }

    @Test
    fun `Saksbehandler har tilgang dersom person er skjermet og saksbehandler er i en gruppe som har tilgang`() {
        EgenAnsattTilgangsKontroll(
            tillatteGrupper = setOf("A", "B"),
            erEgenAnssattFun = { true },
        ).let {
            it.harTilgang(UUIDv7.ny(), Saksbehandler("ident", setOf("B"))) shouldBe true
            it.harTilgang(UUIDv7.ny(), Saksbehandler("ident", setOf("A", "B"))) shouldBe true
            it.harTilgang(UUIDv7.ny(), Saksbehandler("ident", setOf("A", "B", "C"))) shouldBe true
        }
    }

    @Test
    fun `Saksbehandler har IKKE tilgang dersom person er skjermet og saksbehandler IKKE er i en gruppe som har tilgang`() {
        EgenAnsattTilgangsKontroll(
            tillatteGrupper = setOf("A", "B"),
            erEgenAnssattFun = { true },
        ).harTilgang(UUIDv7.ny(), Saksbehandler("ident", setOf("C"))) shouldBe false
    }
}
