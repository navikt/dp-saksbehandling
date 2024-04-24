package no.nav.dagpenger.saksbehandling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.OPPRETTET
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.OppgaveAnsvarHendelse
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.UUID

class OppgaveTilstandTest {
    private val oppgaveId = UUIDv7.ny()
    private val testIdent = "12345699999"

    @Test
    fun `Skal ikke kunne tildele oppgave i tilstand Opprettet`() {
        val oppgave = lagOppgave(oppgaveId = oppgaveId, ident = testIdent)
        oppgave.tilstand() shouldBe OPPRETTET
        shouldThrow<IllegalStateException> {
            oppgave.tildel(OppgaveAnsvarHendelse(oppgaveId, "Z080808"))
        }
    }

    @Test
    fun `Skal kunne sette oppgave klar til behandling i tilstand opprettet`() {
        val oppgave = lagOppgave(oppgaveId = oppgaveId, ident = testIdent)
        oppgave.tilstand() shouldBe OPPRETTET
        oppgave.oppgaveKlarTilBehandling(ForslagTilVedtakHendelse(ident = testIdent, søknadId = UUIDv7.ny(), behandlingId = UUIDv7.ny()))
        oppgave.tilstand() shouldBe KLAR_TIL_BEHANDLING
    }

    @Test
    fun `Skal ikke kunne fjerne oppgaveansvar i tilstand Klar til behandling`() {
        val oppgave = lagOppgave(oppgaveId = oppgaveId, ident = testIdent)
        oppgave.tilstand() shouldBe OPPRETTET
        oppgave.oppgaveKlarTilBehandling(ForslagTilVedtakHendelse(ident = testIdent, søknadId = UUIDv7.ny(), behandlingId = UUIDv7.ny()))
        oppgave.tilstand() shouldBe KLAR_TIL_BEHANDLING

        shouldThrow<IllegalStateException> {
            oppgave.fjernAnsvar(OppgaveAnsvarHendelse(oppgaveId, "Z080808"))
        }
    }

    private fun lagOppgave(oppgaveId: UUID, ident: String) = Oppgave(
        oppgaveId = oppgaveId,
        ident = ident,
        behandlingId = UUIDv7.ny(),
        opprettet = ZonedDateTime.now(),
    )
}
