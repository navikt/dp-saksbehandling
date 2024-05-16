package no.nav.dagpenger.saksbehandling

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.OPPRETTET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.PAA_VENT
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.UlovligTilstandsendringException
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.OppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.UtsettOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.VedtakFattetHendelse
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.LocalDate
import java.time.LocalDateTime

class OppgaveTilstandTest {
    private val oppgaveId = UUIDv7.ny()
    private val testIdent = "12345699999"

    @Test
    fun `Skal på nytt kunne tildele en tildelt oppgave til samme saksbehandler`() {
        val saksbehandlerIdent = "Z080808"
        val oppgave = lagOppgave(UNDER_BEHANDLING, saksbehandlerIdent)

        shouldNotThrow<Oppgave.AlleredeTildeltException> {
            oppgave.tildel(OppgaveAnsvarHendelse(oppgaveId, saksbehandlerIdent))
        }

        shouldThrow<Oppgave.AlleredeTildeltException> {
            oppgave.tildel(OppgaveAnsvarHendelse(oppgaveId, "enAnnenSaksbehandler"))
        }
    }

    @Test
    fun `Skal ikke kunne tildele oppgave i tilstand Opprettet`() {
        val oppgave = lagOppgave(OPPRETTET)
        shouldThrow<UlovligTilstandsendringException> {
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
        oppgave.tilstand().type shouldBe KLAR_TIL_BEHANDLING
    }

    @ParameterizedTest
    @EnumSource(Type::class)
    fun `Skal kunne ferdigstill en oppgave fra alle tilstander`(type: Type) {
        val oppgave = lagOppgave(type)
        oppgave.ferdigstill(
            VedtakFattetHendelse(behandlingId = oppgave.behandlingId, søknadId = UUIDv7.ny(), ident = testIdent),
        )
        oppgave.tilstand().type shouldBe FERDIG_BEHANDLET
    }

    @Test
    fun `Skal gå til KlarTilBehandling fra UnderBehandling`() {
        val oppgave =
            Oppgave.rehydrer(
                oppgaveId = UUIDv7.ny(),
                ident = "ident",
                saksbehandlerIdent = "saksbehandlerIdent",
                behandlingId = UUIDv7.ny(),
                opprettet = LocalDateTime.now(),
                emneknagger = setOf(),
                tilstand = Oppgave.UnderBehandling,
                behandling = behandling,
                utsattTil = null,
            )

        shouldNotThrowAny {
            oppgave.fjernAnsvar(OppgaveAnsvarHendelse(oppgaveId, "Z080808"))
        }

        oppgave.tilstand().type shouldBe KLAR_TIL_BEHANDLING
        oppgave.saksbehandlerIdent shouldBe null
    }

    @Test
    fun `Skal ikke kunne fjerne oppgaveansvar i tilstand Klar til behandling`() {
        val oppgave = lagOppgave(OPPRETTET)
        oppgave.tilstand().type shouldBe OPPRETTET
        oppgave.oppgaveKlarTilBehandling(
            ForslagTilVedtakHendelse(
                ident = testIdent,
                søknadId = UUIDv7.ny(),
                behandlingId = UUIDv7.ny(),
            ),
        )
        oppgave.tilstand().type shouldBe KLAR_TIL_BEHANDLING

        shouldThrow<UlovligTilstandsendringException> {
            oppgave.fjernAnsvar(OppgaveAnsvarHendelse(oppgaveId, "Z080808"))
        }
    }

    @Test
    fun `Skal ikke endre tilstand på en oppgave når den er ferdigbehandlet`() {
        val oppgave = lagOppgave(FERDIG_BEHANDLET)

        shouldThrow<UlovligTilstandsendringException> {
            oppgave.oppgaveKlarTilBehandling(
                ForslagTilVedtakHendelse(
                    ident = testIdent,
                    søknadId = UUIDv7.ny(),
                    behandlingId = UUIDv7.ny(),
                ),
            )
        }
        shouldThrow<UlovligTilstandsendringException> {
            oppgave.fjernAnsvar(OppgaveAnsvarHendelse(UUIDv7.ny(), "Z080808"))
        }

        shouldThrow<UlovligTilstandsendringException> {
            oppgave.tildel(OppgaveAnsvarHendelse(UUIDv7.ny(), "Z080808"))
        }
    }

    @Test
    fun `Skal kunne utsette en opppgave og ta den tilbake til under behandling`() {
        val saksbehandlerIdent = "Z080808"
        val oppgave = lagOppgave(UNDER_BEHANDLING, saksbehandlerIdent)
        val utsattTil = LocalDate.now().plusDays(1)

        oppgave.utsett(
            UtsettOppgaveHendelse(
                oppgaveId = oppgave.oppgaveId,
                navIdent = saksbehandlerIdent,
                utsattTil = utsattTil,
                beholdOppgave = false,
            ),
        )

        oppgave.tilstand() shouldBe Oppgave.PaaVent
        oppgave.utsattTil() shouldBe utsattTil
        oppgave.saksbehandlerIdent shouldBe null

        oppgave.tildel(OppgaveAnsvarHendelse(oppgaveId, saksbehandlerIdent))

        oppgave.tilstand() shouldBe Oppgave.UnderBehandling
        oppgave.utsattTil() shouldBe null
        oppgave.saksbehandlerIdent shouldBe saksbehandlerIdent
    }

    @Test
    fun `Saksbehandler skal kunne beholde en oppgave når den settes PAA_VENT`() {
        val saksbehandlerIdent = "Z080808"
        val oppgave = lagOppgave(UNDER_BEHANDLING, saksbehandlerIdent)
        val utsattTil = LocalDate.now().plusDays(1)

        oppgave.utsett(
            UtsettOppgaveHendelse(
                oppgaveId = oppgave.oppgaveId,
                navIdent = saksbehandlerIdent,
                utsattTil = utsattTil,
                beholdOppgave = true,
            ),
        )

        oppgave.tilstand() shouldBe Oppgave.PaaVent
        oppgave.utsattTil() shouldBe utsattTil
        oppgave.saksbehandlerIdent shouldBe saksbehandlerIdent
    }

    @Test
    fun `En utsatt oppgave skal kunne settes tilbake til KLAR TIL Behandling`() {
        val saksbehandlerIdent = "Z080808"
        val oppgave = lagOppgave(UNDER_BEHANDLING, saksbehandlerIdent)
        val utSattTil = LocalDate.now().plusDays(1)

        oppgave.utsett(
            UtsettOppgaveHendelse(
                oppgaveId = oppgave.oppgaveId,
                navIdent = saksbehandlerIdent,
                utsattTil = utSattTil,
                beholdOppgave = false,
            ),
        )

        oppgave.settTilbakeTilKlarTilBehandling()

        oppgave.tilstand() shouldBe Oppgave.KlarTilBehandling
        oppgave.utsattTil() shouldBe null
        oppgave.saksbehandlerIdent shouldBe null
    }

    @Test
    fun `Fjern ansvar fra en oppgave med tilstand PAA_VENT`() {
        val saksbehandlerIdent = "Z080808"
        val oppgave = lagOppgave(UNDER_BEHANDLING, saksbehandlerIdent)
        val utSattTil = LocalDate.now().plusDays(1)

        oppgave.utsett(
            UtsettOppgaveHendelse(
                oppgaveId = oppgave.oppgaveId,
                navIdent = saksbehandlerIdent,
                utsattTil = utSattTil,
                beholdOppgave = false,
            ),
        )

        oppgave.fjernAnsvar(
            OppgaveAnsvarHendelse(
                oppgaveId = oppgave.oppgaveId,
                navIdent = saksbehandlerIdent,
            ),
        )

        oppgave.tilstand() shouldBe Oppgave.KlarTilBehandling
        oppgave.utsattTil() shouldBe null
        oppgave.saksbehandlerIdent shouldBe null
    }

    private val behandling =
        Behandling(
            behandlingId = UUIDv7.ny(),
            person = Person(id = UUIDv7.ny(), ident = "12345678910"),
            opprettet = LocalDateTime.now(),
        )

    private fun lagOppgave(
        type: Type,
        saksbehandlerIdent: String? = null,
    ): Oppgave {
        val tilstand =
            when (type) {
                OPPRETTET -> Oppgave.Opprettet
                KLAR_TIL_BEHANDLING -> Oppgave.KlarTilBehandling
                FERDIG_BEHANDLET -> Oppgave.FerdigBehandlet
                UNDER_BEHANDLING -> Oppgave.UnderBehandling
                PAA_VENT -> Oppgave.PaaVent
            }
        return Oppgave.rehydrer(
            oppgaveId = UUIDv7.ny(),
            ident = "ident",
            behandlingId = UUIDv7.ny(),
            emneknagger = setOf(),
            opprettet = LocalDateTime.now(),
            tilstand = tilstand,
            saksbehandlerIdent = saksbehandlerIdent,
            behandling = behandling,
            utsattTil = null,
        )
    }
}
