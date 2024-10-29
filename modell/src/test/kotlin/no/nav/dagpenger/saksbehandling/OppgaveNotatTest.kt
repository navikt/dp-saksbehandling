package no.nav.dagpenger.saksbehandling

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_KONTROLL
import no.nav.dagpenger.saksbehandling.OppgaveTestHelper.lagOppgave
import no.nav.dagpenger.saksbehandling.hendelser.NotatHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import org.junit.jupiter.api.Test

class OppgaveNotatTest {
    private val oppgaveId = UUIDv7.ny()

    @Test
    fun `Oppgave er KLAR_TIL_KONTROLL, og skal gå til UNDER_KONTROLL`() {
        val beslutter = Saksbehandler("beslutterIdent", emptySet(), setOf(TilgangType.BESLUTTER))
        val oppgave = lagOppgave(KLAR_TIL_KONTROLL, null)

        oppgave.tildel(
            SettOppgaveAnsvarHendelse(
                oppgaveId = oppgave.oppgaveId,
                ansvarligIdent = beslutter.navIdent,
                utførtAv = beslutter,
            ),
        )

        oppgave.tilstand().notat() shouldBe null

        oppgave.lagreNotat(NotatHendelse(oppgaveId, "Hei", beslutter))
        oppgave.tilstand().notat()!!.hentTekst() shouldBe "Hei"
        oppgave.lagreNotat(NotatHendelse(oppgaveId, "Hei På", beslutter))
        oppgave.tilstand().notat()!!.hentTekst() shouldBe "Hei På"

        oppgave.tildel(
            SettOppgaveAnsvarHendelse(
                oppgaveId = oppgave.oppgaveId,
                ansvarligIdent = beslutter.navIdent,
                utførtAv = beslutter,
            ),
        )
        oppgave.tilstand().notat()!!.hentTekst() shouldBe "Hei På"
    }
}
