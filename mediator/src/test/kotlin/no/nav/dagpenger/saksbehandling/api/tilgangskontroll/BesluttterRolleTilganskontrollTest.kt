package no.nav.dagpenger.saksbehandling.api.tilgangskontroll

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.UUIDv7
import org.junit.jupiter.api.Test

class BesluttterRolleTilganskontrollTest {
    @Test
    fun `beslutter har tilgang`() {
        BeslutterRolleTilgangskontroll.harTilgang(
            oppgaveId = UUIDv7.ny(),
            saksbehandler =
                Saksbehandler(
                    navIdent = "123",
                    grupper = setOf("BeslutterADGruppe"),
                ),
        ) shouldBe true
    }

    @Test
    fun `saksbehandler  har ikke tilgang`() {
        BeslutterRolleTilgangskontroll.harTilgang(
            oppgaveId = UUIDv7.ny(),
            saksbehandler =
                Saksbehandler(
                    navIdent = "123",
                    grupper = setOf("SaksbehandlerADGruppe"),
                ),
        ) shouldBe false
    }
}
