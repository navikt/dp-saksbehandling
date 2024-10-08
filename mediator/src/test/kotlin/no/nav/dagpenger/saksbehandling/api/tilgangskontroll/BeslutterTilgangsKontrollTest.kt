package no.nav.dagpenger.saksbehandling.api.tilgangskontroll

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.UUIDv7
import org.junit.jupiter.api.Test

class BeslutterTilgangsKontrollTest {
    @Test
    fun `beslutter har tilgang`() {
        BeslutterTilgangskontroll.harTilgang(
            oppgaveId = UUIDv7.ny(),
            saksbehandler =
                Saksbehandler(
                    navIdent = "123",
                    grupper = setOf("BeslutterADGruppe"),
                    token = "token",
                ),
        ) shouldBe true
    }

    @Test
    fun `saksbehandler  har ikke tilgang`() {
        BeslutterTilgangskontroll.harTilgang(
            oppgaveId = UUIDv7.ny(),
            saksbehandler =
                Saksbehandler(
                    navIdent = "123",
                    grupper = setOf("SaksbehandlerADGruppe"),
                    token = "token",
                ),
        ) shouldBe false
    }
}
