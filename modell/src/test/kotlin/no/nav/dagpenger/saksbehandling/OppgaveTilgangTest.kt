package no.nav.dagpenger.saksbehandling

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.ManglendeTilgang
import no.nav.dagpenger.saksbehandling.hendelser.GodkjentBehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ToTrinnskontrollHendelse
import org.junit.jupiter.api.Test

class OppgaveTilgangTest {
    private val saksbehandler =
        Saksbehandler(
            navIdent = "saksbehandler",
            grupper = setOf(),
            tilganger = setOf(TilgangType.SAKSBEHANDLER),
        )

    private val beslutter =
        Saksbehandler(
            navIdent = "beslutter",
            grupper = setOf(),
            tilganger = setOf(TilgangType.BESLUTTER),
        )

    @Test
    fun `Må ha beslutter tilgang for å ferdigstill oppgave med tilstand UNDDER_KONTROLL`() {
        val oppgave =
            OppgaveTestHelper.lagOppgave(
                tilstandType = Oppgave.Tilstand.Type.UNDER_KONTROLL,
                behandler = saksbehandler,
            )

        shouldThrow<ManglendeTilgang> {
            oppgave.ferdigstill(
                GodkjentBehandlingHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    meldingOmVedtak = "<HTML>en melding</HTML>",
                    utførtAv = saksbehandler,
                ),
            )
        }

        shouldNotThrowAny {
            oppgave.ferdigstill(
                GodkjentBehandlingHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    meldingOmVedtak = "<HTML>en melding</HTML>",
                    utførtAv = beslutter,
                ),
            )
        }
    }

    @Test
    fun `Må ha beslutter tilgang for å ta oppgave med tilstand KLAR_TIL_KONTROLL`() {
        val saksbehandler =
            Saksbehandler(
                navIdent = "saksbehandler",
                grupper = setOf(),
                tilganger = setOf(TilgangType.SAKSBEHANDLER),
            )

        val beslutter =
            Saksbehandler(
                navIdent = "beslutter",
                grupper = setOf(),
                tilganger = setOf(TilgangType.BESLUTTER),
            )

        val oppgave =
            OppgaveTestHelper.lagOppgave(
                tilstandType = Oppgave.Tilstand.Type.KLAR_TIL_KONTROLL,
            )

        shouldThrow<ManglendeTilgang> {
            oppgave.tildelTotrinnskontroll(
                toTrinnskontrollHendelse =
                    ToTrinnskontrollHendelse(
                        oppgaveId = oppgave.oppgaveId,
                        ansvarligIdent = saksbehandler.navIdent,
                        utførtAv = saksbehandler,
                    ),
            )
        }

        shouldNotThrowAny {
            oppgave.tildelTotrinnskontroll(
                toTrinnskontrollHendelse =
                    ToTrinnskontrollHendelse(
                        oppgaveId = oppgave.oppgaveId,
                        ansvarligIdent = beslutter.navIdent,
                        utførtAv = beslutter,
                    ),
            )
        }
    }
}
