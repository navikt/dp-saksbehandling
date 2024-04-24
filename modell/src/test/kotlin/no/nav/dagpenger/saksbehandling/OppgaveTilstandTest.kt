package no.nav.dagpenger.saksbehandling

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.OPPRETTET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.OppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.ZonedDateTime

class OppgaveTilstandTest {
    private val oppgaveId = UUIDv7.ny()
    private val testIdent = "12345699999"

    @Test
    fun `Skal ikke kunne tildele oppgave i tilstand Opprettet`() {
        val oppgave = lagOppgave(OPPRETTET)
        shouldThrow<IllegalStateException> {
            oppgave.tildel(OppgaveAnsvarHendelse(oppgaveId, "Z080808"))
        }
    }

    @Test
    fun `Skal kunne sette oppgave klar til behandling i tilstand opprettet`() {
        val oppgave = lagOppgave(OPPRETTET)
        oppgave.oppgaveKlarTilBehandling(
            ForslagTilVedtakHendelse(
                ident = testIdent,
                søknadId = UUIDv7.ny(),
                behandlingId = UUIDv7.ny(),
            ),
        )
        oppgave.tilstand() shouldBe KLAR_TIL_BEHANDLING
    }

    @ParameterizedTest
    @EnumSource(Type::class)
    fun `Skal kunne ferdigstill en oppgave fra alle tilstander`(type: Type) {
        val oppgave = lagOppgave(type)
        oppgave.ferdigstill(
            VedtakFattetHendelse(behandlingId = oppgave.behandlingId, søknadId = UUIDv7.ny(), ident = testIdent),
        )
        oppgave.tilstand() shouldBe FERDIG_BEHANDLET
    }

    @Test
    fun `Skal gå til KlarTilBehandling fra UnderBehandling`() {
        val oppgave = Oppgave.rehydrer(
            oppgaveId = UUIDv7.ny(),
            ident = "ident",
            saksbehandlerIdent = "saksbehandlerIdent",
            behandlingId = UUIDv7.ny(),
            opprettet = ZonedDateTime.now(),
            emneknagger = setOf(),
            tilstand = Oppgave.UnderBehandling,
        )

        shouldNotThrowAny {
            oppgave.fjernAnsvar(OppgaveAnsvarHendelse(oppgaveId, "Z080808"))
        }

        oppgave.tilstand() shouldBe KLAR_TIL_BEHANDLING
        oppgave.saksbehandlerIdent shouldBe null
    }

    @Test
    fun `Skal ikke kunne fjerne oppgaveansvar i tilstand Klar til behandling`() {
        val oppgave = lagOppgave(OPPRETTET)
        oppgave.tilstand() shouldBe OPPRETTET
        oppgave.oppgaveKlarTilBehandling(
            ForslagTilVedtakHendelse(
                ident = testIdent,
                søknadId = UUIDv7.ny(),
                behandlingId = UUIDv7.ny(),
            ),
        )
        oppgave.tilstand() shouldBe KLAR_TIL_BEHANDLING

        shouldThrow<IllegalStateException> {
            oppgave.fjernAnsvar(OppgaveAnsvarHendelse(oppgaveId, "Z080808"))
        }
    }

    private fun lagOppgave(type: Type): Oppgave {
        val tilstand = when (type) {
            OPPRETTET -> Oppgave.Opprettet
            KLAR_TIL_BEHANDLING -> Oppgave.KlarTilBehandling
            FERDIG_BEHANDLET -> Oppgave.FerdigBehandlet
            UNDER_BEHANDLING -> Oppgave.UnderBehandling
        }
        return Oppgave(
            oppgaveId = UUIDv7.ny(),
            ident = "ident",
            behandlingId = UUIDv7.ny(),
            emneknagger = setOf(),
            opprettet = ZonedDateTime.now(),
            tilstand = tilstand,
        )
    }
}
