package no.nav.dagpenger.saksbehandling

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_KONTROLL
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.OPPRETTET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.PAA_VENT
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_KONTROLL
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.UlovligTilstandsendringException
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.GodkjennBehandlingMedBrevIArena
import no.nav.dagpenger.saksbehandling.hendelser.GodkjentBehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.OppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.TilKontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.TilbakeTilKontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ToTrinnskontrollHendelse
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
    private val sak = Sak("12342", "Arena")

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

    @Test
    fun `Skal kunne ferdigstille en oppgave fra alle lovlige tilstander`() {
        val lovligeTilstander = setOf(PAA_VENT, UNDER_BEHANDLING, OPPRETTET, KLAR_TIL_BEHANDLING, FERDIG_BEHANDLET)
        lovligeTilstander.forEach { tilstand ->
            val oppgave = lagOppgave(tilstand)
            oppgave.ferdigstill(
                VedtakFattetHendelse(
                    behandlingId = oppgave.behandlingId,
                    søknadId = UUIDv7.ny(),
                    ident = testIdent,
                    sak = sak,
                ),
            )
            oppgave.tilstand().type shouldBe FERDIG_BEHANDLET
        }

        (Type.values.toMutableSet() - lovligeTilstander).forEach { tilstand ->
            val oppgave = lagOppgave(tilstand)
            shouldThrow<UlovligTilstandsendringException> {
                oppgave.ferdigstill(
                    VedtakFattetHendelse(
                        behandlingId = oppgave.behandlingId,
                        søknadId = UUIDv7.ny(),
                        ident = testIdent,
                        sak = sak,
                    ),
                )
            }
        }
    }

    @Test
    fun `Skal gå til KlarTilBehandling fra UnderBehandling`() {
        val oppgave = lagOppgave(type = KLAR_TIL_BEHANDLING)

        shouldNotThrowAny {
            oppgave.fjernAnsvar(OppgaveAnsvarHendelse(oppgaveId, "Z080808"))
        }

        oppgave.tilstand().type shouldBe KLAR_TIL_BEHANDLING
        oppgave.saksbehandlerIdent shouldBe null
    }

    @Test
    fun `Skal gå til FERDIG_BEHANDLET fra UNDER_BEHANDLING vha GodkjentBehandlingHendelse`() {
        val oppgave = lagOppgave(type = UNDER_BEHANDLING, saksbehandlerIdent = "HuggaBugga")

        oppgave.ferdigstill(
            godkjentBehandlingHendelse =
                GodkjentBehandlingHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    meldingOmVedtak = "Melding om vedtak",
                    saksbehandlerToken = "token",
                ),
        )

        oppgave.tilstand().type shouldBe FERDIG_BEHANDLET
        oppgave.saksbehandlerIdent shouldBe "HuggaBugga"
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

    @Test
    fun `Fjern ansvar fra en oppgave med tilstand KLAR_TIL_BEHANDLING`() {
        val saksbehandlerIdent = "Z080808"
        val oppgave = lagOppgave(KLAR_TIL_BEHANDLING, saksbehandlerIdent)

        oppgave.fjernAnsvar(
            OppgaveAnsvarHendelse(
                oppgaveId = oppgave.oppgaveId,
                navIdent = saksbehandlerIdent,
            ),
        )

        oppgave.tilstand() shouldBe Oppgave.KlarTilBehandling
        oppgave.saksbehandlerIdent shouldBe null
    }

    @Test
    fun `Skal gå fra tilstand UNDER_BEHANDLING til KLAR_TIL_KONTROLL`() {
        val saksbehandlerIdent = "Z080808"
        val oppgave = lagOppgave(UNDER_BEHANDLING, saksbehandlerIdent)

        oppgave.gjørKlarTilKontroll(
            TilKontrollHendelse(),
        )

        oppgave.tilstand() shouldBe Oppgave.KlarTilKontroll
        oppgave.saksbehandlerIdent shouldBe null
    }

    @ParameterizedTest
    @EnumSource(Type::class)
    fun `Ulovlige tilstandsendringer til KLAR_TIL_KONTROLL`(tilstandstype: Type) {
        val saksbehandlerIdent = "Z080808"
        val oppgave = lagOppgave(type = tilstandstype, saksbehandlerIdent)

        if (tilstandstype != UNDER_BEHANDLING) {
            shouldThrow<UlovligTilstandsendringException> {
                oppgave.gjørKlarTilKontroll(
                    TilKontrollHendelse(),
                )
            }
        }
    }

    @Test
    fun `Skal gå fra tilstand KLAR_TIL_KONTROLL til UNDER_KONTROLL`() {
        val beslutterIdent = "Z080808"
        val oppgave = lagOppgave(KLAR_TIL_KONTROLL, null)

        oppgave.tildelTotrinnskontroll(
            ToTrinnskontrollHendelse(
                beslutterIdent = beslutterIdent,
            ),
        )

        oppgave.tilstand() shouldBe Oppgave.UnderKontroll
        oppgave.saksbehandlerIdent shouldBe beslutterIdent
    }

    @ParameterizedTest
    @EnumSource(Type::class)
    fun `Ulovlige tilstandsendringer til UNDER_KONTROLL`(tilstandstype: Type) {
        val oppgave = lagOppgave(type = tilstandstype, "hubba")
        if (tilstandstype != KLAR_TIL_KONTROLL) {
            shouldThrow<UlovligTilstandsendringException> {
                oppgave.tildelTotrinnskontroll(
                    ToTrinnskontrollHendelse(
                        beslutterIdent = "hubba",
                    ),
                )
            }
        }
    }

    @Test
    fun `Skal gå fra tilstand UNDER_KONTROLL til FERDIG_BEHANDLET`() {
        val beslutterIdent = "Z080808"
        val oppgave = lagOppgave(UNDER_KONTROLL, beslutterIdent)

        oppgave.ferdigstill(
            godkjentBehandlingHendelse =
                GodkjentBehandlingHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    meldingOmVedtak = "Melding om vedtak",
                    saksbehandlerToken = "token",
                ),
        )

        oppgave.tilstand() shouldBe Oppgave.FerdigBehandlet
        oppgave.saksbehandlerIdent shouldBe beslutterIdent

        val oppgave2 = lagOppgave(UNDER_KONTROLL, beslutterIdent)

        oppgave2.ferdigstill(
            godkjennBehandlingMedBrevIArena =
                GodkjennBehandlingMedBrevIArena(
                    oppgaveId = oppgave.oppgaveId,
                    saksbehandlerToken = "økøk",
                ),
        )

        oppgave2.tilstand() shouldBe Oppgave.FerdigBehandlet
        oppgave2.saksbehandlerIdent shouldBe beslutterIdent
    }

    @Test
    fun `Skal gå fra tilstand UNDER_KONTROLL til UNDER_BEHANDLING`() {
        val beslutterIdent = "Z080808"
        val saksbehandlerIdent = "Z080809"
        val oppgave = lagOppgave(UNDER_KONTROLL, beslutterIdent)

        oppgave.sendTilbakeTilUnderBehandling(
            oppgaveAnsvarHendelse =
                OppgaveAnsvarHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    navIdent = saksbehandlerIdent,
                ),
        )

        oppgave.tilstand() shouldBe Oppgave.UnderBehandling
        oppgave.saksbehandlerIdent shouldBe saksbehandlerIdent
    }

    @Test
    fun `Skal gå fra tilstand UNDER_KONTROLL til KLAR_TIL_KONTROLL`() {
        val beslutterIdent = "Z080808"
        val oppgave = lagOppgave(UNDER_KONTROLL, beslutterIdent)

        oppgave.sendTilbakeTilKlarTilKontroll(
            tilbakeTilKontrollHendelse =
                TilbakeTilKontrollHendelse(),
        )

        oppgave.tilstand() shouldBe Oppgave.KlarTilKontroll
        oppgave.saksbehandlerIdent shouldBe null
    }

    private val behandling =
        Behandling(
            behandlingId = UUIDv7.ny(),
            person =
                Person(
                    id = UUIDv7.ny(),
                    ident = "12345678910",
                    skjermesSomEgneAnsatte = false,
                    adressebeskyttelseGradering = UGRADERT,
                ),
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
                KLAR_TIL_KONTROLL -> Oppgave.KlarTilKontroll
                UNDER_KONTROLL -> Oppgave.UnderKontroll
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
