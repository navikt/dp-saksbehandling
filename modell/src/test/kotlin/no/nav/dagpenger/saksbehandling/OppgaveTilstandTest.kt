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
import no.nav.dagpenger.saksbehandling.hendelser.KlarTilKontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.OppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.TilbakeTilKontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.TilbakeTilUnderKontrollHendelse
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
    private val saksbehandler = Aktør.Saksbehandler("sbIdent")
    private val sak = Sak("12342", "Arena")

    @Test
    fun `Skal på nytt kunne tildele en tildelt oppgave til samme saksbehandler`() {
        val oppgave = lagOppgave(UNDER_BEHANDLING, saksbehandler.navIdent)

        shouldNotThrow<Oppgave.AlleredeTildeltException> {
            oppgave.tildel(OppgaveAnsvarHendelse(oppgaveId, saksbehandler.navIdent, saksbehandler))
        }
        val enAnnenSaksbehandler = Aktør.Saksbehandler("enAnnenSaksbehandler")
        shouldThrow<Oppgave.AlleredeTildeltException> {
            oppgave.tildel(OppgaveAnsvarHendelse(oppgaveId, enAnnenSaksbehandler.navIdent, enAnnenSaksbehandler))
        }
    }

    @Test
    fun `Skal ikke kunne tildele oppgave i tilstand Opprettet`() {
        val oppgave = lagOppgave(OPPRETTET)
        shouldThrow<UlovligTilstandsendringException> {
            oppgave.tildel(OppgaveAnsvarHendelse(oppgaveId, saksbehandler.navIdent, saksbehandler))
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
                aktør = Aktør.System.dpBehandling,
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
                    aktør = Aktør.Saksbehandler("sbIdent"),
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
                        aktør = Aktør.System.dpBehandling,
                    ),
                )
            }
        }
    }

    @Test
    fun `Skal gå til KlarTilBehandling fra UnderBehandling`() {
        val oppgave = lagOppgave(type = KLAR_TIL_BEHANDLING)

        shouldNotThrowAny {
            oppgave.fjernAnsvar(OppgaveAnsvarHendelse(oppgaveId, saksbehandler.navIdent, saksbehandler))
        }

        oppgave.tilstand().type shouldBe KLAR_TIL_BEHANDLING
        oppgave.saksbehandlerIdent shouldBe null
    }

    @Test
    fun `Skal gå til FERDIG_BEHANDLET fra UNDER_BEHANDLING vha GodkjentBehandlingHendelse`() {
        val saksbehandler = Aktør.Saksbehandler("sbIdent")
        val oppgave = lagOppgave(type = UNDER_BEHANDLING, saksbehandlerIdent = saksbehandler.navIdent)

        oppgave.ferdigstill(
            godkjentBehandlingHendelse =
                GodkjentBehandlingHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    meldingOmVedtak = "Melding om vedtak",
                    saksbehandlerToken = "token",
                    aktør = saksbehandler,
                ),
        )

        oppgave.tilstand().type shouldBe FERDIG_BEHANDLET
        oppgave.saksbehandlerIdent shouldBe saksbehandler.navIdent
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
                    aktør = Aktør.System.dpBehandling,
                ),
            )
        }
        shouldThrow<UlovligTilstandsendringException> {
            oppgave.fjernAnsvar(OppgaveAnsvarHendelse(UUIDv7.ny(), saksbehandler.navIdent, saksbehandler))
        }

        shouldThrow<UlovligTilstandsendringException> {
            oppgave.tildel(OppgaveAnsvarHendelse(UUIDv7.ny(), saksbehandler.navIdent, saksbehandler))
        }
    }

    @Test
    fun `Skal kunne utsette en opppgave og ta den tilbake til under behandling`() {
        val oppgave = lagOppgave(UNDER_BEHANDLING, saksbehandler.navIdent)
        val utsattTil = LocalDate.now().plusDays(1)

        oppgave.utsett(
            UtsettOppgaveHendelse(
                oppgaveId = oppgave.oppgaveId,
                navIdent = saksbehandler.navIdent,
                utsattTil = utsattTil,
                beholdOppgave = false,
                aktør = saksbehandler,
            ),
        )

        oppgave.tilstand() shouldBe Oppgave.PaaVent
        oppgave.utsattTil() shouldBe utsattTil
        oppgave.saksbehandlerIdent shouldBe null

        oppgave.tildel(OppgaveAnsvarHendelse(oppgaveId, saksbehandler.navIdent, saksbehandler))

        oppgave.tilstand() shouldBe Oppgave.UnderBehandling
        oppgave.utsattTil() shouldBe null
        oppgave.saksbehandlerIdent shouldBe saksbehandler.navIdent
    }

    @Test
    fun `Saksbehandler skal kunne beholde en oppgave når den settes PAA_VENT`() {
        val oppgave = lagOppgave(UNDER_BEHANDLING, saksbehandler.navIdent)
        val utsattTil = LocalDate.now().plusDays(1)

        oppgave.utsett(
            UtsettOppgaveHendelse(
                oppgaveId = oppgave.oppgaveId,
                navIdent = saksbehandler.navIdent,
                utsattTil = utsattTil,
                beholdOppgave = true,
                aktør = saksbehandler,
            ),
        )

        oppgave.tilstand() shouldBe Oppgave.PaaVent
        oppgave.utsattTil() shouldBe utsattTil
        oppgave.saksbehandlerIdent shouldBe saksbehandler.navIdent
    }

    @Test
    fun `Fjern ansvar fra en oppgave med tilstand PAA_VENT`() {
        val oppgave = lagOppgave(UNDER_BEHANDLING, saksbehandler.navIdent)
        val utSattTil = LocalDate.now().plusDays(1)

        oppgave.utsett(
            UtsettOppgaveHendelse(
                oppgaveId = oppgave.oppgaveId,
                navIdent = saksbehandler.navIdent,
                utsattTil = utSattTil,
                beholdOppgave = false,
                aktør = saksbehandler,
            ),
        )

        oppgave.fjernAnsvar(
            OppgaveAnsvarHendelse(
                oppgaveId = oppgave.oppgaveId,
                navIdent = saksbehandler.navIdent,
                aktør = saksbehandler,
            ),
        )

        oppgave.tilstand() shouldBe Oppgave.KlarTilBehandling
        oppgave.utsattTil() shouldBe null
        oppgave.saksbehandlerIdent shouldBe null
    }

    @Test
    fun `Fjern ansvar fra en oppgave med tilstand KLAR_TIL_BEHANDLING`() {
        val oppgave = lagOppgave(KLAR_TIL_BEHANDLING, saksbehandler.navIdent)

        oppgave.fjernAnsvar(
            OppgaveAnsvarHendelse(
                oppgaveId = oppgave.oppgaveId,
                navIdent = saksbehandler.navIdent,
                aktør = saksbehandler,
            ),
        )

        oppgave.tilstand() shouldBe Oppgave.KlarTilBehandling
        oppgave.saksbehandlerIdent shouldBe null
    }

    @Test
    fun `Skal gå fra tilstand UNDER_BEHANDLING til KLAR_TIL_KONTROLL`() {
        val oppgave = lagOppgave(UNDER_BEHANDLING, saksbehandler.navIdent)

        oppgave.gjørKlarTilKontroll(
            KlarTilKontrollHendelse(utførtAv = saksbehandler),
        )

        oppgave.tilstand() shouldBe Oppgave.KlarTilKontroll
        oppgave.saksbehandlerIdent shouldBe null
    }

    @ParameterizedTest
    @EnumSource(Type::class)
    fun `Ulovlige tilstandsendringer til KLAR_TIL_KONTROLL`(tilstandstype: Type) {
        val oppgave = lagOppgave(type = tilstandstype, saksbehandler.navIdent)

        if (tilstandstype != UNDER_BEHANDLING) {
            shouldThrow<UlovligTilstandsendringException> {
                oppgave.gjørKlarTilKontroll(
                    KlarTilKontrollHendelse(utførtAv = Aktør.System.dpSaksbehandling),
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

    @Test
    fun `Skal gå fra tilstand UNDER_BEHANDLING til UNDER_KONTROLL`() {
        val beslutterIdent = "Z080808"
        val saksbehandlerIdent = "Z999999"
        val oppgave = lagOppgave(UNDER_BEHANDLING, saksbehandlerIdent)

        oppgave.sendTilbakeTilUnderKontroll(
            TilbakeTilUnderKontrollHendelse(beslutterIdent),
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
        val beslutter = Aktør.Beslutter(beslutterIdent)
        oppgave.ferdigstill(
            godkjentBehandlingHendelse =
                GodkjentBehandlingHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    meldingOmVedtak = "Melding om vedtak",
                    saksbehandlerToken = "token",
                    aktør = beslutter,
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
                    aktør = beslutter,
                ),
        )

        oppgave2.tilstand() shouldBe Oppgave.FerdigBehandlet
        oppgave2.saksbehandlerIdent shouldBe beslutterIdent
    }

    @Test
    fun `Skal gå fra tilstand UNDER_KONTROLL til UNDER_BEHANDLING`() {
        val beslutter = Aktør.Beslutter("besluttterIdent")
        val saksbehandlerIdent = "Z080809"
        val oppgave = lagOppgave(UNDER_KONTROLL, beslutter.navIdent)

        oppgave.sendTilbakeTilUnderBehandling(
            oppgaveAnsvarHendelse =
                OppgaveAnsvarHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    navIdent = saksbehandlerIdent,
                    aktør = beslutter,
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
                TilbakeTilKontrollHendelse(aktør = Aktør.Beslutter(beslutterIdent)),
        )

        oppgave.tilstand() shouldBe Oppgave.KlarTilKontroll
        oppgave.saksbehandlerIdent shouldBe null
    }

    @Test
    fun `Finn saksbehandler og beslutter på oppgaven`() {
        val oppgave = lagOppgave(OPPRETTET)
        oppgave.oppgaveKlarTilBehandling(
            ForslagTilVedtakHendelse(
                ident = testIdent,
                søknadId = UUIDv7.ny(),
                behandlingId = UUIDv7.ny(),
                aktør = Aktør.System.dpBehandling,
            ),
        )

        oppgave.sisteSaksbehandler() shouldBe null

        val saksbehandlerAktør1 = Aktør.Saksbehandler("saksbehandler 1")
        oppgave.tildel(OppgaveAnsvarHendelse(oppgaveId, saksbehandlerAktør1.navIdent, saksbehandlerAktør1))
        oppgave.sisteSaksbehandler() shouldBe saksbehandlerAktør1.navIdent

        oppgave.fjernAnsvar(OppgaveAnsvarHendelse(oppgaveId, saksbehandlerAktør1.navIdent, saksbehandlerAktør1))
        oppgave.sisteSaksbehandler() shouldBe saksbehandlerAktør1.navIdent

        val saksbehandlerAktør2 = Aktør.Saksbehandler("saksbehandler 2")
        oppgave.tildel(OppgaveAnsvarHendelse(oppgaveId, saksbehandlerAktør2.navIdent, saksbehandlerAktør2))
        oppgave.sisteSaksbehandler() shouldBe saksbehandlerAktør2.navIdent

        oppgave.gjørKlarTilKontroll(KlarTilKontrollHendelse(utførtAv = saksbehandlerAktør2))

        val beslutterAktør1 = Aktør.Beslutter("beslutter 1")
        oppgave.tildelTotrinnskontroll(ToTrinnskontrollHendelse(beslutterIdent = beslutterAktør1.navIdent))
        oppgave.sisteBeslutter() shouldBe beslutterAktør1.navIdent

        oppgave.sendTilbakeTilUnderBehandling(OppgaveAnsvarHendelse(oppgaveId, saksbehandlerAktør2.navIdent, beslutterAktør1))
        oppgave.sisteBeslutter() shouldBe beslutterAktør1.navIdent
        oppgave.sisteSaksbehandler() shouldBe saksbehandlerAktør2.navIdent

        val beslutterAktør2 = Aktør.Beslutter("beslutter 2")
        oppgave.gjørKlarTilKontroll(KlarTilKontrollHendelse(utførtAv = saksbehandlerAktør2))
        oppgave.tildelTotrinnskontroll(ToTrinnskontrollHendelse(beslutterIdent = beslutterAktør2.navIdent))
        oppgave.sisteBeslutter() shouldBe beslutterAktør2.navIdent
        oppgave.sisteSaksbehandler() shouldBe saksbehandlerAktør2.navIdent

        oppgave.ferdigstill(
            godkjentBehandlingHendelse =
                GodkjentBehandlingHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    meldingOmVedtak = "Melding om vedtak",
                    saksbehandlerToken = "token",
                    aktør = beslutterAktør2,
                ),
        )
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
