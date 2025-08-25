package no.nav.dagpenger.saksbehandling

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.Oppgave.MeldingOmVedtakKilde.GOSYS
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_KONTROLL
import no.nav.dagpenger.saksbehandling.OppgaveTestHelper.lagOppgave
import no.nav.dagpenger.saksbehandling.hendelser.EndreMeldingOmVedtakKildeHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import org.junit.jupiter.api.Test

class OppgaveMeldingOmVedtakKildeTest {
    private val oppgaveId = UUIDv7.ny()

    @Test
    fun   `Skal endre melding om vedtak kilde`() {
        val saksbehandler = Saksbehandler("saksbehandler", emptySet(), )
        val oppgave = lagOppgave(Oppgave.Tilstand.Type.UNDER_BEHANDLING, saksbehandler)

        oppgave.endreMeldingOmVedtakKilde(
            EndreMeldingOmVedtakKildeHendelse(
                oppgaveId = oppgave.oppgaveId,
                meldingOmVedtakKilde = GOSYS,
                utførtAv = saksbehandler,
            ),
        )

        oppgave.tilstand().notat() shouldBe null
    }
}