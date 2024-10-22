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
import no.nav.dagpenger.saksbehandling.hendelser.FjernOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.GodkjennBehandlingMedBrevIArena
import no.nav.dagpenger.saksbehandling.hendelser.GodkjentBehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlarTilKontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.NesteOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.TilbakeTilKlarTilKontrollHendelse
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
    private val saksbehandler = Saksbehandler("sbIdent", grupper = emptySet())
    private val sak = Sak("12342", "Arena")

    @Test
    fun `Skal på nytt kunne tildele en tildelt oppgave til samme saksbehandler`() {
        val oppgave = lagOppgave(UNDER_BEHANDLING, saksbehandler)

        shouldNotThrow<Oppgave.AlleredeTildeltException> {
            oppgave.tildel(SettOppgaveAnsvarHendelse(oppgaveId, saksbehandler.navIdent, saksbehandler))
        }
        val enAnnenSaksbehandler = Saksbehandler("enAnnenSaksbehandler", emptySet())
        shouldThrow<Oppgave.AlleredeTildeltException> {
            oppgave.tildel(SettOppgaveAnsvarHendelse(oppgaveId, enAnnenSaksbehandler.navIdent, enAnnenSaksbehandler))
        }
    }

    @Test
    fun `Skal ikke kunne tildele oppgave i tilstand Opprettet`() {
        val oppgave = lagOppgave(OPPRETTET)
        shouldThrow<UlovligTilstandsendringException> {
            oppgave.tildel(SettOppgaveAnsvarHendelse(oppgaveId, saksbehandler.navIdent, saksbehandler))
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
                utførtAv = Applikasjon("dp-behandling"),
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
        val oppgave = lagOppgave(tilstandType = UNDER_BEHANDLING, behandler = saksbehandler)

        shouldNotThrowAny {
            oppgave.fjernAnsvar(FjernOppgaveAnsvarHendelse(oppgaveId, saksbehandler))
        }

        oppgave.tilstand().type shouldBe KLAR_TIL_BEHANDLING
        oppgave.behandlerIdent shouldBe null
    }

    @Test
    fun `Skal gå til FERDIG_BEHANDLET fra UNDER_BEHANDLING vha GodkjentBehandlingHendelse`() {
        val saksbehandler = Saksbehandler("sIdent", emptySet())
        val oppgave = lagOppgave(tilstandType = UNDER_BEHANDLING, behandler = saksbehandler)

        oppgave.ferdigstill(
            godkjentBehandlingHendelse =
                GodkjentBehandlingHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    meldingOmVedtak = "Melding om vedtak",
                    utførtAv = saksbehandler,
                ),
        )

        oppgave.tilstand().type shouldBe FERDIG_BEHANDLET
        oppgave.behandlerIdent shouldBe saksbehandler.navIdent
    }

    @Test
    fun `Skal endre emneknagger hvis nytt forslag til vedtak mottas i tilstand KLAR_TILBEHANDLING`() {
        val oppgave = lagOppgave(KLAR_TIL_BEHANDLING)
        val emneknagger = setOf("knagg1", "knagg2")
        shouldNotThrow<Exception> {
            oppgave.oppgaveKlarTilBehandling(
                ForslagTilVedtakHendelse(
                    ident = testIdent,
                    søknadId = UUIDv7.ny(),
                    behandlingId = oppgave.behandling.behandlingId,
                    utførtAv = Applikasjon("dp-behandling"),
                    emneknagger = emneknagger,
                ),
            )
        }
        oppgave.emneknagger shouldBe emneknagger
        oppgave.tilstand() shouldBe Oppgave.KlarTilBehandling
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
                    utførtAv = Applikasjon("dp-behandling"),
                ),
            )
        }
        shouldThrow<UlovligTilstandsendringException> {
            oppgave.fjernAnsvar(FjernOppgaveAnsvarHendelse(UUIDv7.ny(), saksbehandler))
        }

        shouldThrow<UlovligTilstandsendringException> {
            oppgave.tildel(SettOppgaveAnsvarHendelse(UUIDv7.ny(), saksbehandler.navIdent, saksbehandler))
        }
    }

    @Test
    fun `Skal kunne utsette en opppgave og ta den tilbake til under behandling`() {
        val oppgave = lagOppgave(UNDER_BEHANDLING, saksbehandler)
        val utsattTil = LocalDate.now().plusDays(1)

        oppgave.utsett(
            UtsettOppgaveHendelse(
                oppgaveId = oppgave.oppgaveId,
                navIdent = saksbehandler.navIdent,
                utsattTil = utsattTil,
                beholdOppgave = false,
                utførtAv = saksbehandler,
            ),
        )

        oppgave.tilstand() shouldBe Oppgave.PaaVent
        oppgave.utsattTil() shouldBe utsattTil
        oppgave.behandlerIdent shouldBe null

        oppgave.tildel(SettOppgaveAnsvarHendelse(oppgaveId, saksbehandler.navIdent, saksbehandler))

        oppgave.tilstand() shouldBe Oppgave.UnderBehandling
        oppgave.utsattTil() shouldBe null
        oppgave.behandlerIdent shouldBe saksbehandler.navIdent
    }

    @Test
    fun `Saksbehandler skal kunne beholde en oppgave når den settes PAA_VENT`() {
        val oppgave = lagOppgave(UNDER_BEHANDLING, saksbehandler)
        val utsattTil = LocalDate.now().plusDays(1)

        oppgave.utsett(
            UtsettOppgaveHendelse(
                oppgaveId = oppgave.oppgaveId,
                navIdent = saksbehandler.navIdent,
                utsattTil = utsattTil,
                beholdOppgave = true,
                utførtAv = saksbehandler,
            ),
        )

        oppgave.tilstand() shouldBe Oppgave.PaaVent
        oppgave.utsattTil() shouldBe utsattTil
        oppgave.behandlerIdent shouldBe saksbehandler.navIdent
    }

    @Test
    fun `Fjern ansvar fra en oppgave med tilstand PAA_VENT`() {
        val oppgave = lagOppgave(UNDER_BEHANDLING, saksbehandler)
        val utSattTil = LocalDate.now().plusDays(1)

        oppgave.utsett(
            UtsettOppgaveHendelse(
                oppgaveId = oppgave.oppgaveId,
                navIdent = saksbehandler.navIdent,
                utsattTil = utSattTil,
                beholdOppgave = false,
                utførtAv = saksbehandler,
            ),
        )

        oppgave.fjernAnsvar(
            FjernOppgaveAnsvarHendelse(
                oppgaveId = oppgave.oppgaveId,
                utførtAv = saksbehandler,
            ),
        )

        oppgave.tilstand() shouldBe Oppgave.KlarTilBehandling
        oppgave.utsattTil() shouldBe null
        oppgave.behandlerIdent shouldBe null
    }

    @Test
    fun `Skal gå fra tilstand UNDER_BEHANDLING til KLAR_TIL_KONTROLL`() {
        val oppgave = lagOppgave(UNDER_BEHANDLING, saksbehandler)

        oppgave.gjørKlarTilKontroll(
            KlarTilKontrollHendelse(oppgaveId = oppgave.oppgaveId, utførtAv = saksbehandler),
        )

        oppgave.tilstand() shouldBe Oppgave.KlarTilKontroll
        oppgave.behandlerIdent shouldBe null
    }

    @ParameterizedTest
    @EnumSource(Type::class)
    fun `Ulovlige tilstandsendringer til KLAR_TIL_KONTROLL`(tilstandstype: Type) {
        val oppgave = lagOppgave(tilstandType = tilstandstype, saksbehandler)

        if (tilstandstype != UNDER_BEHANDLING) {
            shouldThrow<UlovligTilstandsendringException> {
                oppgave.gjørKlarTilKontroll(
                    KlarTilKontrollHendelse(
                        oppgaveId = oppgave.oppgaveId,
                        utførtAv = Saksbehandler("ident", emptySet()),
                    ),
                )
            }
        }
    }

    @Test
    fun `Skal gå fra tilstand KLAR_TIL_KONTROLL til UNDER_KONTROLL`() {
        val beslutter = Saksbehandler("beslutterIdent", emptySet())
        val oppgave = lagOppgave(KLAR_TIL_KONTROLL, null)

        oppgave.tildelTotrinnskontroll(
            ToTrinnskontrollHendelse(
                oppgaveId = oppgave.oppgaveId,
                ansvarligIdent = beslutter.navIdent,
                utførtAv = beslutter,
            ),
        )
        oppgave.tilstand() shouldBe Oppgave.UnderKontroll
        oppgave.behandlerIdent shouldBe beslutter.navIdent
    }

    @Test
    fun `Skal gå fra tilstand UNDER_BEHANDLING til UNDER_KONTROLL`() {
        val beslutterIdent = Saksbehandler("Z080808", emptySet())
        val saksbehandlerIdent = Saksbehandler("Z999999", emptySet())
        val oppgave = lagOppgave(UNDER_BEHANDLING, saksbehandlerIdent)

        oppgave.sendTilbakeTilUnderKontroll(
            TilbakeTilUnderKontrollHendelse(
                oppgaveId = oppgave.oppgaveId,
                ansvarligIdent = beslutterIdent.navIdent,
                utførtAv = saksbehandlerIdent,
            ),
        )

        oppgave.tilstand() shouldBe Oppgave.UnderKontroll
        oppgave.behandlerIdent shouldBe beslutterIdent.navIdent
    }

    @ParameterizedTest
    @EnumSource(Type::class)
    fun `Ulovlige tilstandsendringer til UNDER_KONTROLL`(tilstandstype: Type) {
        val beslutter = Saksbehandler("beslutterIdent", emptySet())
        val oppgave = lagOppgave(tilstandType = tilstandstype, beslutter)
        if (tilstandstype != KLAR_TIL_KONTROLL) {
            shouldThrow<UlovligTilstandsendringException> {
                oppgave.tildelTotrinnskontroll(
                    ToTrinnskontrollHendelse(
                        oppgaveId = oppgave.oppgaveId,
                        ansvarligIdent = beslutter.navIdent,
                        utførtAv = beslutter,
                    ),
                )
            }
        }
    }

    @Test
    fun `Skal kunne ferdigstille med brev i ny løsning fra UNDER_KONTROLL`() {
        val beslutter = Saksbehandler("Z080808", emptySet())
        val oppgave = lagOppgave(UNDER_KONTROLL, beslutter)
        oppgave.ferdigstill(
            godkjentBehandlingHendelse =
                GodkjentBehandlingHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    meldingOmVedtak = "Melding om vedtak",
                    utførtAv = beslutter,
                ),
        )

        oppgave.tilstand() shouldBe Oppgave.FerdigBehandlet
        oppgave.behandlerIdent shouldBe beslutter.navIdent
    }

    @Test
    fun `Skal ikke kunne ferdigstille med brev i Arena fra UNDER_KONTROLL`() {
        val beslutter = Saksbehandler("Z080808", emptySet())
        val oppgave = lagOppgave(UNDER_KONTROLL, beslutter)

        shouldThrow<UlovligTilstandsendringException> {
            oppgave.ferdigstill(
                godkjennBehandlingMedBrevIArena =
                    GodkjennBehandlingMedBrevIArena(
                        oppgaveId = oppgave.oppgaveId,
                        utførtAv = beslutter,
                    ),
            )
        }
    }

    @Test
    fun `Skal gå fra tilstand UNDER_KONTROLL til UNDER_BEHANDLING`() {
        val beslutter = Saksbehandler("besluttterIdent", emptySet())
        val saksbehandlerIdent = "Z080809"
        val oppgave = lagOppgave(UNDER_KONTROLL, beslutter)

        oppgave.sendTilbakeTilUnderBehandling(
            settOppgaveAnsvarHendelse =
                SettOppgaveAnsvarHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    ansvarligIdent = saksbehandlerIdent,
                    utførtAv = beslutter,
                ),
        )

        oppgave.tilstand() shouldBe Oppgave.UnderBehandling
        oppgave.behandlerIdent shouldBe saksbehandlerIdent
    }

    @Test
    fun `Skal gå fra tilstand UNDER_KONTROLL til KLAR_TIL_KONTROLL`() {
        val beslutter = Saksbehandler("Z080808", emptySet())
        val oppgave = lagOppgave(UNDER_KONTROLL, beslutter)

        oppgave.sendTilbakeTilKlarTilKontroll(
            tilbakeTilKontrollHendelse =
                TilbakeTilKlarTilKontrollHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    utførtAv = beslutter,
                ),
        )

        oppgave.tilstand() shouldBe Oppgave.KlarTilKontroll
        oppgave.behandlerIdent shouldBe null
    }

    @Test
    fun `Finn siste saksbehandler når oppgave er tildelt via neste-oppgave funksjon`() {
        val saksbehandler = Saksbehandler("Z080808", emptySet())
        val oppgave =
            lagOppgave(
                tilstandType = UNDER_BEHANDLING,
                tilstandslogg =
                    Tilstandslogg().also {
                        it.leggTil(
                            nyTilstand = KLAR_TIL_BEHANDLING,
                            hendelse =
                                ForslagTilVedtakHendelse(
                                    ident = "11111155555",
                                    søknadId = UUIDv7.ny(),
                                    behandlingId = UUIDv7.ny(),
                                    emneknagger = emptySet(),
                                ),
                        )
                        it.leggTil(
                            nyTilstand = UNDER_BEHANDLING,
                            hendelse =
                                NesteOppgaveHendelse(
                                    ansvarligIdent = saksbehandler.navIdent,
                                    utførtAv = saksbehandler,
                                ),
                        )
                    },
            )
        oppgave.sisteSaksbehandler() shouldBe saksbehandler.navIdent
    }

    @Test
    fun `Finn saksbehandler og beslutter på oppgaven`() {
        val oppgave = lagOppgave(OPPRETTET)
        val oppgaveId = oppgave.oppgaveId
        oppgave.oppgaveKlarTilBehandling(
            ForslagTilVedtakHendelse(
                ident = testIdent,
                søknadId = UUIDv7.ny(),
                behandlingId = UUIDv7.ny(),
                utførtAv = Applikasjon("dp-behandling"),
            ),
        )

        oppgave.sisteSaksbehandler() shouldBe null

        val saksbehandler1 = Saksbehandler("saksbehandler 1", emptySet())
        oppgave.tildel(SettOppgaveAnsvarHendelse(oppgaveId, saksbehandler1.navIdent, saksbehandler1))
        oppgave.sisteSaksbehandler() shouldBe saksbehandler1.navIdent

        oppgave.fjernAnsvar(FjernOppgaveAnsvarHendelse(oppgaveId, saksbehandler1))
        oppgave.sisteSaksbehandler() shouldBe saksbehandler1.navIdent

        val saksbehandler2 = Saksbehandler("saksbehandler 2", emptySet())
        oppgave.tildel(SettOppgaveAnsvarHendelse(oppgaveId, saksbehandler2.navIdent, saksbehandler2))
        oppgave.sisteSaksbehandler() shouldBe saksbehandler2.navIdent

        oppgave.gjørKlarTilKontroll(KlarTilKontrollHendelse(oppgaveId = oppgaveId, utførtAv = saksbehandler2))

        val beslutter1 = Saksbehandler("beslutter 1", emptySet())
        oppgave.tildelTotrinnskontroll(
            ToTrinnskontrollHendelse(
                oppgaveId = oppgave.oppgaveId,
                ansvarligIdent = beslutter1.navIdent,
                utførtAv = beslutter1,
            ),
        )
        oppgave.sisteBeslutter() shouldBe beslutter1.navIdent

        oppgave.sendTilbakeTilUnderBehandling(SettOppgaveAnsvarHendelse(oppgaveId, saksbehandler2.navIdent, beslutter1))
        oppgave.sisteBeslutter() shouldBe beslutter1.navIdent
        oppgave.sisteSaksbehandler() shouldBe saksbehandler2.navIdent

        val beslutter2 = Saksbehandler("beslutter 2", emptySet())
        oppgave.gjørKlarTilKontroll(KlarTilKontrollHendelse(oppgaveId = oppgave.oppgaveId, utførtAv = saksbehandler2))
        oppgave.tildelTotrinnskontroll(
            ToTrinnskontrollHendelse(
                oppgaveId = oppgave.oppgaveId,
                ansvarligIdent = beslutter2.navIdent,
                utførtAv = beslutter1,
            ),
        )
        oppgave.sisteBeslutter() shouldBe beslutter2.navIdent
        oppgave.sisteSaksbehandler() shouldBe saksbehandler2.navIdent

        oppgave.ferdigstill(
            godkjentBehandlingHendelse =
                GodkjentBehandlingHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    meldingOmVedtak = "Melding om vedtak",
                    utførtAv = beslutter2,
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
        tilstandType: Type,
        behandler: Saksbehandler? = null,
        tilstandslogg: Tilstandslogg = Tilstandslogg(),
    ): Oppgave {
        val tilstand =
            when (tilstandType) {
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
            behandlerIdent = behandler?.navIdent,
            behandling = behandling,
            utsattTil = null,
            tilstandslogg = tilstandslogg,
        )
    }
}
