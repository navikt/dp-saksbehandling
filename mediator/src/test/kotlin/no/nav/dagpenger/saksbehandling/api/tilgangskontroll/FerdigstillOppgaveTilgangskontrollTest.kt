package no.nav.dagpenger.saksbehandling.api.tilgangskontroll

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.db.lagOppgave
import org.junit.jupiter.api.Test

class FerdigstillOppgaveTilgangskontrollTest {
    private val saksbehandler = Saksbehandler("saksbehandler", setOf("SaksbehandlerADGruppe"))
    private val beslutter =
        Saksbehandler("beslutter", setOf("SaksbehandlerADGruppe", "BeslutterADGruppe"))
    private val annenBeslutter =
        Saksbehandler("annenBeslutter", setOf("SaksbehandlerADGruppe", "BeslutterADGruppe"))

    @Test
    fun `Dersom en oppgave er i tilstand UNDER_BEHANDLING, skal en saksbehandler ha tilgang dersom de er eier av oppgaven`() {
        val oppgave = lagOppgave(tilstand = Oppgave.UnderBehandling, saksbehandlerIdent = saksbehandler.navIdent)

        FerdigstillOppgaveTilgangskontroll { oppgave }.let {
            it.harTilgang(oppgaveId = oppgave.oppgaveId, saksbehandler = saksbehandler) shouldBe true
            it.harTilgang(oppgaveId = oppgave.oppgaveId, saksbehandler = beslutter) shouldBe false
        }
    }

    @Test
    fun `Dersom en oppgave er i tilstand UNDER_KONTROLL, skal kun en  beslutter ha tilgang dersom de er eier av oppgaven`() {
        val oppgave = lagOppgave(tilstand = Oppgave.UnderKontroll, saksbehandlerIdent = beslutter.navIdent)
        FerdigstillOppgaveTilgangskontroll { oppgave }.let {
            it.harTilgang(oppgaveId = oppgave.oppgaveId, saksbehandler = beslutter) shouldBe true
            it.harTilgang(oppgaveId = oppgave.oppgaveId, saksbehandler = saksbehandler) shouldBe false
            it.harTilgang(oppgaveId = oppgave.oppgaveId, saksbehandler = annenBeslutter) shouldBe false
        }
    }
}
