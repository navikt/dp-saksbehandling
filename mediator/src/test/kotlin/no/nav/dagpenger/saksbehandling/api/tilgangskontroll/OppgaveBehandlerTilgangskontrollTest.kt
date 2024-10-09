package no.nav.dagpenger.saksbehandling.api.tilgangskontroll

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.UUIDv7
import org.junit.jupiter.api.Test

class OppgaveBehandlerTilgangskontrollTest {
    @Test
    fun `Bare saksbehandler som er eier av en oppgave har tilgang`() {
        val oppgaveId = UUIDv7.ny()
        val saksbehandlerA = Saksbehandler("Saksbehandler A", emptySet())
        val saksbehandlerB = Saksbehandler("Saksbehandler B", emptySet())
        OppgaveBehandlerTilgangskontroll(
            behandlerFunc = { saksbehandlerA.navIdent },
        ).let {
            it.harTilgang(oppgaveId, saksbehandlerA) shouldBe true
            it.harTilgang(oppgaveId, saksbehandlerB) shouldBe false
        }

        OppgaveBehandlerTilgangskontroll(
            behandlerFunc = { null },
        ).let {
            it.harTilgang(oppgaveId, saksbehandlerA) shouldBe false
        }
    }
}
