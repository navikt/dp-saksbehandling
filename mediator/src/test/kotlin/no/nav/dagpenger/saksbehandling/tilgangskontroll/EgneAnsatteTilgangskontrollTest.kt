package no.nav.dagpenger.saksbehandling.tilgangskontroll

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.api.Saksbehandler
import org.junit.jupiter.api.Test

class EgneAnsatteTilgangskontrollTest {
    @Test
    fun `Saksbehandler har alltid tilgang dersom oppgaven tilhører en person som ikke er egen ansatt`() {
        EgneAnsatteTilgangskontroll(
            tillatteGrupper = setOf("A", "B"),
            erEgenAnsattFun = { false },
        ).let {
            it.harTilgang(UUIDv7.ny(), Saksbehandler("ident", emptySet())) shouldBe true
            it.harTilgang(UUIDv7.ny(), Saksbehandler("ident", setOf("C"))) shouldBe true
            it.harTilgang(UUIDv7.ny(), Saksbehandler("ident", setOf("A"))) shouldBe true
        }
    }

    @Test
    fun `Saksbehandler gis tilgang dersom person er egen ansatt og saksbehandler har tilgang til gruppa`() {
        EgneAnsatteTilgangskontroll(
            tillatteGrupper = setOf("A", "B"),
            erEgenAnsattFun = { true },
        ).let {
            it.harTilgang(UUIDv7.ny(), Saksbehandler("ident", setOf("B"))) shouldBe true
            it.harTilgang(UUIDv7.ny(), Saksbehandler("ident", setOf("A", "B"))) shouldBe true
            it.harTilgang(UUIDv7.ny(), Saksbehandler("ident", setOf("A", "B", "C"))) shouldBe true
        }
    }

    @Test
    fun `Saksbehandler gis ikke tilgang dersom person er egen ansatt og saksbehandler mangler tilgang til gruppa`() {
        EgneAnsatteTilgangskontroll(
            tillatteGrupper = setOf("A", "B"),
            erEgenAnsattFun = { true },
        ).harTilgang(UUIDv7.ny(), Saksbehandler("ident", setOf("C"))) shouldBe false
    }
}
