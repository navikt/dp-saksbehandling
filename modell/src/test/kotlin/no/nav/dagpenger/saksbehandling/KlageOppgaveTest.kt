package no.nav.dagpenger.saksbehandling

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.hendelser.FerdigstillKlageOppgave
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class KlageOppgaveTest {
    private val saksbehandler =
        Saksbehandler(
            navIdent = "Z123",
            grupper = emptySet(),
            tilganger = emptySet(),
        )

    private val oppgaveId: UUID = UUIDv7.ny()

    @Test
    fun `Livsyklus på klage oppgavens tilstander`() {
        val klageOppgave =
            KlageOppgave(
                oppgaveId = UUIDv7.ny(),
                opprettet = LocalDateTime.now(),
            ).also {
                it.tilstand() shouldBe KlageOppgave.KlarTilBehandling
            }

        klageOppgave.tildel(
            SettOppgaveAnsvarHendelse(
                oppgaveId = oppgaveId,
                ansvarligIdent = saksbehandler.navIdent,
                utførtAv = saksbehandler,
            ),
        )
        klageOppgave.tilstand() shouldBe KlageOppgave.UnderBehandling

        klageOppgave.ferdigstill(FerdigstillKlageOppgave(utførtAv = saksbehandler))
        klageOppgave.tilstand() shouldBe KlageOppgave.FerdigBehandlet
    }
}
