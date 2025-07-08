package no.nav.dagpenger.saksbehandling

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.Oppgave.AlleredeTildeltException
import no.nav.dagpenger.saksbehandling.Oppgave.Companion.RETUR_FRA_KONTROLL
import no.nav.dagpenger.saksbehandling.Oppgave.Companion.kontrollEmneknagger
import no.nav.dagpenger.saksbehandling.Oppgave.Companion.påVentEmneknagger
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.ManglendeTilgang
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.BEHANDLES_I_ARENA
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_KONTROLL
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.OPPRETTET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.PAA_VENT
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_KONTROLL
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.UlovligTilstandsendringException
import no.nav.dagpenger.saksbehandling.OppgaveTestHelper.lagOppgave
import no.nav.dagpenger.saksbehandling.TilgangType.BESLUTTER
import no.nav.dagpenger.saksbehandling.TilgangType.SAKSBEHANDLER
import no.nav.dagpenger.saksbehandling.hendelser.AvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.BehandlingAvbruttHendelse
import no.nav.dagpenger.saksbehandling.hendelser.FjernOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ForslagTilVedtakHendelse
import no.nav.dagpenger.saksbehandling.hendelser.GodkjennBehandlingMedBrevIArena
import no.nav.dagpenger.saksbehandling.hendelser.GodkjentBehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.NesteOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ReturnerTilSaksbehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SendTilKontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
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
    private val utsendingSak = UtsendingSak("12342", "Arena")

    @Test
    fun `Skal på nytt kunne tildele en tildelt oppgave til samme saksbehandler`() {
        val oppgave = lagOppgave(UNDER_BEHANDLING, saksbehandler)

        shouldNotThrow<AlleredeTildeltException> {
            oppgave.tildel(SettOppgaveAnsvarHendelse(oppgaveId, saksbehandler.navIdent, saksbehandler))
        }
        val enAnnenSaksbehandler = Saksbehandler("enAnnenSaksbehandler", emptySet())
        shouldThrow<AlleredeTildeltException> {
            oppgave.tildel(SettOppgaveAnsvarHendelse(oppgaveId, enAnnenSaksbehandler.navIdent, enAnnenSaksbehandler))
        }
    }

    @Test
    fun `Skal på nytt kunne tildele en oppgave UnderKontroll til samme beslutter`() {
        val beslutter = Saksbehandler("beslutter", emptySet(), setOf(BESLUTTER))
        val oppgave = lagOppgave(tilstandType = UNDER_KONTROLL, beslutter)
        shouldNotThrow<AlleredeTildeltException> {
            oppgave.tildel(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    ansvarligIdent = beslutter.navIdent,
                    utførtAv = beslutter,
                ),
            )
        }
        shouldNotThrow<ManglendeTilgang> {
            oppgave.tildel(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    ansvarligIdent = beslutter.navIdent,
                    utførtAv = beslutter,
                ),
            )
        }
        oppgave.tilstand().type shouldBe UNDER_KONTROLL
    }

    @Test
    fun `Skal ikke kunne tildele oppgave i tilstand Opprettet`() {
        val oppgave = lagOppgave(OPPRETTET)
        shouldThrow<UlovligTilstandsendringException> {
            oppgave.tildel(SettOppgaveAnsvarHendelse(oppgaveId, saksbehandler.navIdent, saksbehandler))
        }
    }

    @Test
    fun `Skal kun endre tilstand dersom forslag til vedtak mottas i tilstand Opprettet`() {
        val hendelse =
            ForslagTilVedtakHendelse(
                ident = testIdent,
                id = UUIDv7.ny().toString(),
                behandletHendelseType = "Søknad",
                behandlingId = UUIDv7.ny(),
                utførtAv = Applikasjon("dp-behandling"),
            )

        lagOppgave(tilstandType = OPPRETTET).let { oppgave ->
            oppgave.oppgaveKlarTilBehandling(hendelse) shouldBe Oppgave.Handling.LAGRE_OPPGAVE
            oppgave.tilstand().type shouldBe KLAR_TIL_BEHANDLING
        }

        setOf(KLAR_TIL_BEHANDLING, PAA_VENT, UNDER_BEHANDLING).forEach { tilstand ->
            lagOppgave(tilstandType = tilstand).let { oppgave ->
                val tilstandFørHendelse = oppgave.tilstand().type
                oppgave.oppgaveKlarTilBehandling(hendelse) shouldBe Oppgave.Handling.LAGRE_OPPGAVE
                oppgave.tilstand().type shouldBe tilstandFørHendelse
            }
        }
    }

    @Test
    fun `Skal ferdigstille en oppgave på bakgrunn av et manuelt fattet vedtak fra alle lovlige tilstander`() {
        val lovligeTilstander =
            setOf(PAA_VENT, UNDER_BEHANDLING, OPPRETTET, KLAR_TIL_BEHANDLING, FERDIG_BEHANDLET, UNDER_KONTROLL)
        lovligeTilstander.forEach { tilstand ->
            val oppgave = lagOppgave(tilstand)
            val resultat =
                oppgave.ferdigstill(
                    VedtakFattetHendelse(
                        behandlingId = oppgave.behandlingId,
                        id = UUIDv7.ny().toString(),
                        behandletHendelseType = "Søknad",
                        ident = testIdent,
                        automatiskBehandlet = false,
                        sak = utsendingSak,
                    ),
                )

            if (tilstand == UNDER_KONTROLL) {
                oppgave.tilstand().type shouldBe UNDER_KONTROLL
            } else if (tilstand == UNDER_BEHANDLING) {
                oppgave.tilstand().type shouldBe UNDER_BEHANDLING
            } else {
                oppgave.tilstand().type shouldBe FERDIG_BEHANDLET
            }

            when (tilstand) {
                in setOf(UNDER_BEHANDLING, UNDER_KONTROLL, FERDIG_BEHANDLET) -> resultat shouldBe Oppgave.Handling.INGEN
                else -> resultat shouldBe Oppgave.Handling.LAGRE_OPPGAVE
            }
        }

        (Type.values.toMutableSet() - lovligeTilstander).forEach { tilstand ->
            val oppgave = lagOppgave(tilstand)
            shouldThrow<UlovligTilstandsendringException> {
                oppgave.ferdigstill(
                    VedtakFattetHendelse(
                        behandlingId = oppgave.behandlingId,
                        id = UUIDv7.ny().toString(),
                        behandletHendelseType = "Søknad",
                        ident = testIdent,
                        automatiskBehandlet = false,
                        sak = utsendingSak,
                    ),
                )
            }
        }
    }

    @Test
    fun `Skal ferdigstille en oppgave på bakgrunn av et automatisk fattet vedtak fra alle lovlige tilstander`() {
        val lovligeTilstander =
            setOf(PAA_VENT, UNDER_BEHANDLING, OPPRETTET, KLAR_TIL_BEHANDLING, FERDIG_BEHANDLET, UNDER_KONTROLL)
        lovligeTilstander.forEach { tilstand ->
            val oppgave = lagOppgave(tilstand)
            val resultat =
                oppgave.ferdigstill(
                    VedtakFattetHendelse(
                        behandlingId = oppgave.behandlingId,
                        id = UUIDv7.ny().toString(),
                        behandletHendelseType = "Søknad",
                        ident = testIdent,
                        sak = utsendingSak,
                        automatiskBehandlet = true,
                    ),
                )

            oppgave.tilstand().type shouldBe FERDIG_BEHANDLET

            when (tilstand) {
                in setOf(FERDIG_BEHANDLET) -> resultat shouldBe Oppgave.Handling.INGEN
                else -> resultat shouldBe Oppgave.Handling.LAGRE_OPPGAVE
            }
        }

        (Type.values.toMutableSet() - lovligeTilstander).forEach { tilstand ->
            val oppgave = lagOppgave(tilstand)
            shouldThrow<UlovligTilstandsendringException> {
                oppgave.ferdigstill(
                    VedtakFattetHendelse(
                        behandlingId = oppgave.behandlingId,
                        id = UUIDv7.ny().toString(),
                        behandletHendelseType = "Søknad",
                        ident = testIdent,
                        automatiskBehandlet = true,
                        sak = utsendingSak,
                    ),
                )
            }
        }
    }

    @Test
    fun `skal ferdigstille en oppgave bassert på avbrytHendelse`() {
        val lovligeTilstander =
            setOf(UNDER_BEHANDLING)

        lovligeTilstander.forEach { tilstand ->
            val oppgave = lagOppgave(tilstand, behandler = saksbehandler)
            shouldNotThrowAny {
                oppgave.ferdigstill(
                    avbruttHendelse =
                        AvbruttHendelse(
                            behandlingId = oppgave.behandlingId,
                            utførtAv = saksbehandler,
                        ),
                )
            }
            oppgave.tilstand().type shouldBe FERDIG_BEHANDLET
        }

        (Type.values.toMutableSet() - lovligeTilstander).forEach { tilstand ->
            val oppgave = lagOppgave(tilstand, behandler = saksbehandler)
            shouldThrow<UlovligTilstandsendringException> {
                oppgave.ferdigstill(
                    avbruttHendelse =
                        AvbruttHendelse(
                            behandlingId = oppgave.behandlingId,
                            utførtAv = saksbehandler,
                        ),
                )
            }
        }
    }

    @Test
    fun `Skal kunne avbryte en oppgave fra alle lovlige tilstander`() {
        val lovligeTilstander =
            setOf(
                OPPRETTET,
                PAA_VENT,
                KLAR_TIL_BEHANDLING,
                UNDER_BEHANDLING,
                KLAR_TIL_KONTROLL,
                UNDER_KONTROLL,
                BEHANDLES_I_ARENA,
            )

        lovligeTilstander.forEach { tilstand ->
            val oppgave = lagOppgave(tilstand, behandler = null)
            shouldNotThrowAny {
                oppgave.behandlesIArena(
                    BehandlingAvbruttHendelse(
                        behandlingId = oppgave.behandlingId,
                        søknadId = UUIDv7.ny(),
                        ident = testIdent,
                    ),
                )
            }
            oppgave.tilstand().type shouldBe BEHANDLES_I_ARENA
        }

        (Type.values.toMutableSet() - lovligeTilstander).forEach { tilstand ->
            val oppgave = lagOppgave(tilstand)
            shouldThrow<UlovligTilstandsendringException> {
                oppgave.behandlesIArena(
                    BehandlingAvbruttHendelse(
                        behandlingId = oppgave.behandlingId,
                        søknadId = UUIDv7.ny(),
                        ident = testIdent,
                    ),
                )
            }
        }
    }

    @Test
    fun `Skal kunne motta forslag til vedtak fra alle lovlige tilstander`() {
        val lovligeTilstander =
            setOf(
                FERDIG_BEHANDLET,
                KLAR_TIL_BEHANDLING,
                OPPRETTET,
                PAA_VENT,
                UNDER_BEHANDLING,
                UNDER_KONTROLL,
            )

        lovligeTilstander.forEach { tilstand ->
            val oppgave = lagOppgave(tilstand, behandler = null)
            shouldNotThrowAny {
                oppgave.oppgaveKlarTilBehandling(
                    ForslagTilVedtakHendelse(
                        behandlingId = oppgave.behandlingId,
                        id = UUIDv7.ny().toString(),
                        behandletHendelseType = "Søknad",
                        ident = testIdent,
                    ),
                )
            }

            when (tilstand) {
                FERDIG_BEHANDLET -> FERDIG_BEHANDLET
                KLAR_TIL_BEHANDLING -> KLAR_TIL_BEHANDLING
                KLAR_TIL_KONTROLL -> KLAR_TIL_KONTROLL
                OPPRETTET -> KLAR_TIL_BEHANDLING
                PAA_VENT -> PAA_VENT
                UNDER_BEHANDLING -> UNDER_BEHANDLING
                UNDER_KONTROLL -> UNDER_KONTROLL
                else -> throw IllegalStateException("Ukjent tilstand")
            }.let { forventetTilstand ->
                oppgave.tilstand().type shouldBe forventetTilstand
            }
        }

        (Type.values.toMutableSet() - lovligeTilstander).forEach { tilstand ->
            val oppgave = lagOppgave(tilstand)
            shouldThrow<UlovligTilstandsendringException> {
                oppgave.oppgaveKlarTilBehandling(
                    ForslagTilVedtakHendelse(
                        behandlingId = oppgave.behandlingId,
                        id = UUIDv7.ny().toString(),
                        behandletHendelseType = "Søknad",
                        ident = testIdent,
                    ),
                )
            }
        }
    }

    @Test
    fun `Skal gå fra UnderBehandling til KlarTilBehandling når oppgaveansvar fjernes`() {
        val oppgave = lagOppgave(tilstandType = UNDER_BEHANDLING, behandler = saksbehandler)

        shouldNotThrowAny {
            oppgave.fjernAnsvar(FjernOppgaveAnsvarHendelse(oppgaveId, saksbehandler))
        }

        oppgave.tilstand().type shouldBe KLAR_TIL_BEHANDLING
        oppgave.behandlerIdent shouldBe null
    }

    @Test
    fun `Skal gå fra UnderBehandling til BehandlesIArena når oppgaven avbrytes`() {
        val oppgave = lagOppgave(tilstandType = UNDER_BEHANDLING, behandler = saksbehandler)

        shouldNotThrowAny {
            oppgave.behandlesIArena(
                BehandlingAvbruttHendelse(
                    behandlingId = oppgave.behandlingId,
                    søknadId = UUIDv7.ny(),
                    ident = testIdent,
                ),
            )
        }
        oppgave.tilstand().type shouldBe BEHANDLES_I_ARENA
    }

    @Test
    fun `Skal gå fra UnderKontroll til KlarTilKontroll når oppgaveansvar fjernes`() {
        val oppgave = lagOppgave(tilstandType = UNDER_KONTROLL, behandler = saksbehandler)

        shouldNotThrowAny {
            oppgave.fjernAnsvar(FjernOppgaveAnsvarHendelse(oppgaveId, saksbehandler))
        }

        oppgave.tilstand().type shouldBe KLAR_TIL_KONTROLL
        oppgave.behandlerIdent shouldBe null
    }

    @Test
    fun `Skal gå fra UnderKontroll til UnderBehandling når beslutter returnerer oppgaven`() {
        val saksbehandler = Saksbehandler("saksbehandlerIdent", emptySet(), setOf(SAKSBEHANDLER))
        val beslutter = Saksbehandler("beslutterIdent", emptySet(), setOf(BESLUTTER))
        val oppgave =
            lagOppgave(
                tilstandType = UNDER_KONTROLL,
                behandler = beslutter,
                tilstandslogg =
                    Tilstandslogg(
                        tilstandsendringer =
                            mutableListOf(
                                Tilstandsendring(
                                    tilstand = UNDER_BEHANDLING,
                                    hendelse =
                                        NesteOppgaveHendelse(
                                            ansvarligIdent = saksbehandler.navIdent,
                                            utførtAv = saksbehandler,
                                        ),
                                    tidspunkt = LocalDateTime.now().minusDays(1),
                                ),
                            ),
                    ),
            )

        shouldNotThrowAny {
            oppgave.returnerTilSaksbehandling(
                ReturnerTilSaksbehandlingHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    utførtAv = beslutter,
                ),
            )
        }

        oppgave.tilstand().type shouldBe UNDER_BEHANDLING
        oppgave.behandlerIdent shouldBe saksbehandler.navIdent
        oppgave.emneknagger.shouldContain(RETUR_FRA_KONTROLL)
    }

    @Test
    fun `Skal gå fra UnderBehandling til FerdigBehandlet når saksbehandler godkjenner en behandling`() {
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
    fun `Skal endre emneknagger hvis nytt forslag til vedtak mottas i tilstand KLAR_TIL_BEHANDLING`() {
        val oppgave =
            lagOppgave(
                KLAR_TIL_BEHANDLING,
                emneknagger = setOf("skalSlettes") + kontrollEmneknagger + påVentEmneknagger,
            )
        val nyeEmneknagger = setOf("knagg1", "knagg2")
        shouldNotThrow<Exception> {
            oppgave.oppgaveKlarTilBehandling(
                ForslagTilVedtakHendelse(
                    ident = testIdent,
                    id = UUIDv7.ny().toString(),
                    behandletHendelseType = "Søknad",
                    behandlingId = oppgave.behandlingId,
                    utførtAv = Applikasjon("dp-behandling"),
                    emneknagger = nyeEmneknagger,
                ),
            )
        }
        oppgave.emneknagger shouldBe nyeEmneknagger + kontrollEmneknagger + påVentEmneknagger
        oppgave.tilstand() shouldBe Oppgave.KlarTilBehandling
    }

    @Test
    fun `Skal endre emneknagger som ikke er kontrollemneknagger hvis nytt forslag til vedtak mottas i tilstand PAA_VENT`() {
        val oppgave = lagOppgave(PAA_VENT, emneknagger = setOf("skalSlettes") + kontrollEmneknagger)
        val nyeEmneknagger = setOf("knagg1", "knagg2")
        shouldNotThrow<Exception> {
            oppgave.oppgaveKlarTilBehandling(
                ForslagTilVedtakHendelse(
                    ident = testIdent,
                    id = UUIDv7.ny().toString(),
                    behandletHendelseType = "Søknad",
                    behandlingId = oppgave.behandlingId,
                    utførtAv = Applikasjon("dp-behandling"),
                    emneknagger = nyeEmneknagger,
                ),
            )
        }
        oppgave.emneknagger shouldBe nyeEmneknagger + kontrollEmneknagger
        oppgave.tilstand() shouldBe Oppgave.PåVent
    }

    @Test
    fun `Skal endre emneknagger som ikke er kontrollemneknagger hvis nytt forslag til vedtak mottas i tilstand UNDER_BEHANDLING`() {
        val oppgave = lagOppgave(UNDER_BEHANDLING, emneknagger = setOf("skalSlettes") + kontrollEmneknagger)
        val nyeEmneknagger = setOf("knagg1", "knagg2")
        shouldNotThrow<Exception> {
            oppgave.oppgaveKlarTilBehandling(
                ForslagTilVedtakHendelse(
                    ident = testIdent,
                    id = UUIDv7.ny().toString(),
                    behandletHendelseType = "Søknad",
                    behandlingId = oppgave.behandlingId,
                    utførtAv = Applikasjon("dp-behandling"),
                    emneknagger = nyeEmneknagger,
                ),
            )
        }
        oppgave.emneknagger shouldBe nyeEmneknagger + kontrollEmneknagger
        oppgave.tilstand() shouldBe Oppgave.UnderBehandling
    }

    @Test
    fun `Ikke tillatt med tildel eller fjern ansvar i tilstand FerdigBehandlet`() {
        val oppgave = lagOppgave(FERDIG_BEHANDLET)

        shouldThrow<UlovligTilstandsendringException> {
            oppgave.fjernAnsvar(FjernOppgaveAnsvarHendelse(UUIDv7.ny(), saksbehandler))
        }

        shouldThrow<UlovligTilstandsendringException> {
            oppgave.tildel(SettOppgaveAnsvarHendelse(UUIDv7.ny(), saksbehandler.navIdent, saksbehandler))
        }
    }

    @Test
    fun `Skal kunne utsette en opppgave uten å beholde den og deretter ta den tilbake UnderBehandling`() {
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

        oppgave.tilstand() shouldBe Oppgave.PåVent
        oppgave.utsattTil() shouldBe utsattTil
        oppgave.behandlerIdent shouldBe null

        oppgave.tildel(SettOppgaveAnsvarHendelse(oppgaveId, saksbehandler.navIdent, saksbehandler))

        oppgave.tilstand() shouldBe Oppgave.UnderBehandling
        oppgave.utsattTil() shouldBe null
        oppgave.behandlerIdent shouldBe saksbehandler.navIdent
    }

    @Test
    fun `Saksbehandler skal kunne beholde en oppgave når den settes på vent`() {
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

        oppgave.tilstand() shouldBe Oppgave.PåVent
        oppgave.utsattTil() shouldBe utsattTil
        oppgave.behandlerIdent shouldBe saksbehandler.navIdent
    }

    @Test
    fun `Fjerning av ansvar fra en utsatt oppgave skal også fjerne datoen det er utsatt til`() {
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
    fun `Skal gå fra UnderBehandling til AvventerLåsAvBehandling når oppgave sendes til kontroll`() {
        val oppgave = lagOppgave(UNDER_BEHANDLING, saksbehandler)

        oppgave.sendTilKontroll(
            SendTilKontrollHendelse(oppgaveId = oppgave.oppgaveId, utførtAv = saksbehandler),
        )

        oppgave.tilstand() shouldBe Oppgave.KlarTilKontroll
        oppgave.behandlerIdent shouldBe null
    }

    @Test
    fun `Skal sette og fjerne aktuell emneknagger når oppgave returneres fra kontroll og sendes til kontroll på nytt`() {
        val oppgave = lagOppgave(KLAR_TIL_BEHANDLING)

        oppgave.tildel(
            SettOppgaveAnsvarHendelse(
                oppgaveId = oppgave.oppgaveId,
                ansvarligIdent = saksbehandler.navIdent,
                utførtAv = saksbehandler,
            ),
        )

        oppgave.sendTilKontroll(
            SendTilKontrollHendelse(oppgaveId = oppgave.oppgaveId, utførtAv = saksbehandler),
        )

        oppgave.tilstand() shouldBe Oppgave.KlarTilKontroll
        oppgave.behandlerIdent shouldBe null
        oppgave.emneknagger shouldNotContain Oppgave.TIDLIGERE_KONTROLLERT
        oppgave.emneknagger shouldNotContain RETUR_FRA_KONTROLL

        val beslutter = Saksbehandler("beslutterIdent", emptySet(), setOf(BESLUTTER))
        oppgave.tildel(
            SettOppgaveAnsvarHendelse(
                oppgaveId = oppgave.oppgaveId,
                ansvarligIdent = beslutter.navIdent,
                utførtAv = beslutter,
            ),
        )
        oppgave.tilstand() shouldBe Oppgave.UnderKontroll()
        oppgave.behandlerIdent shouldBe beslutter.navIdent
        oppgave.emneknagger shouldNotContain Oppgave.TIDLIGERE_KONTROLLERT
        oppgave.emneknagger shouldNotContain RETUR_FRA_KONTROLL

        oppgave.returnerTilSaksbehandling(
            ReturnerTilSaksbehandlingHendelse(
                oppgaveId = oppgave.oppgaveId,
                utførtAv = beslutter,
            ),
        )

        oppgave.tilstand() shouldBe Oppgave.UnderBehandling
        oppgave.behandlerIdent shouldBe saksbehandler.navIdent
        oppgave.emneknagger shouldNotContain Oppgave.TIDLIGERE_KONTROLLERT
        oppgave.emneknagger shouldContain RETUR_FRA_KONTROLL

        oppgave.sendTilKontroll(
            SendTilKontrollHendelse(oppgaveId = oppgave.oppgaveId, utførtAv = saksbehandler),
        )

        oppgave.tilstand() shouldBe Oppgave.UnderKontroll()
        oppgave.behandlerIdent shouldBe beslutter.navIdent
        oppgave.emneknagger shouldContain Oppgave.TIDLIGERE_KONTROLLERT
        oppgave.emneknagger shouldNotContain RETUR_FRA_KONTROLL
    }

    @ParameterizedTest
    @EnumSource(Type::class)
    fun `Ulovlige bruk av sendTilKontroll`(tilstandstype: Type) {
        val oppgave = lagOppgave(tilstandType = tilstandstype, saksbehandler)

        if (tilstandstype != UNDER_BEHANDLING) {
            shouldThrow<UlovligTilstandsendringException> {
                oppgave.sendTilKontroll(
                    SendTilKontrollHendelse(
                        oppgaveId = oppgave.oppgaveId,
                        utførtAv = Saksbehandler("ident", emptySet()),
                    ),
                )
            }
        }
    }

    @Test
    fun `Skal gå fra KlarTilKontroll til UnderKontroll når beslutter tildeles en oppgave`() {
        val beslutter = Saksbehandler("beslutterIdent", emptySet(), setOf(BESLUTTER))
        val oppgave = lagOppgave(KLAR_TIL_KONTROLL, null)

        oppgave.tildel(
            SettOppgaveAnsvarHendelse(
                oppgaveId = oppgave.oppgaveId,
                ansvarligIdent = beslutter.navIdent,
                utførtAv = beslutter,
            ),
        )
        oppgave.tilstand().type shouldBe UNDER_KONTROLL
        oppgave.behandlerIdent shouldBe beslutter.navIdent
    }

    @Test
    fun `Skal gå fra KlarTilBehandling til UnderBehandling når saksbehandler tildeles en oppgave`() {
        val saksbehandler = Saksbehandler("saksbehandler", emptySet(), setOf())
        val oppgave = lagOppgave(tilstandType = KLAR_TIL_BEHANDLING, null)
        oppgave.tildel(
            SettOppgaveAnsvarHendelse(
                oppgaveId = oppgave.oppgaveId,
                ansvarligIdent = saksbehandler.navIdent,
                utførtAv = saksbehandler,
            ),
        )
        oppgave.tilstand() shouldBe Oppgave.UnderBehandling
        oppgave.behandlerIdent shouldBe saksbehandler.navIdent
    }

    @Test
    fun `Skal gå fra KlarTilBehandling til BehandlesIArena når oppgaven avbrytes`() {
        val oppgave = lagOppgave(tilstandType = KLAR_TIL_BEHANDLING, behandler = saksbehandler)

        shouldNotThrowAny {
            oppgave.behandlesIArena(
                BehandlingAvbruttHendelse(
                    behandlingId = oppgave.behandlingId,
                    søknadId = UUIDv7.ny(),
                    ident = testIdent,
                ),
            )
        }
        oppgave.tilstand().type shouldBe BEHANDLES_I_ARENA
    }

    @Test
    fun `Skal gå fra PaaVent til UnderBehandling når saksbehandler tildeles en oppgave`() {
        val saksbehandler = Saksbehandler("saksbehandler", emptySet(), setOf())
        val oppgave = lagOppgave(tilstandType = PAA_VENT, saksbehandler)
        oppgave.tildel(
            SettOppgaveAnsvarHendelse(
                oppgaveId = oppgave.oppgaveId,
                ansvarligIdent = saksbehandler.navIdent,
                utførtAv = saksbehandler,
            ),
        )
        oppgave.tilstand() shouldBe Oppgave.UnderBehandling
    }

    @Test
    fun `Ulovlige tilstandsendringer for tildeling av oppgave`() {
        val beslutter = Saksbehandler("saksbehandler", emptySet(), setOf(SAKSBEHANDLER, BESLUTTER))

        val opprettetOppgave = lagOppgave(tilstandType = OPPRETTET, null)
        shouldThrow<UlovligTilstandsendringException> {
            opprettetOppgave.tildel(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = opprettetOppgave.oppgaveId,
                    ansvarligIdent = beslutter.navIdent,
                    utførtAv = beslutter,
                ),
            )
        }

        val ferdigBehandletOppgave = lagOppgave(tilstandType = FERDIG_BEHANDLET, beslutter)
        shouldThrow<UlovligTilstandsendringException> {
            ferdigBehandletOppgave.tildel(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = ferdigBehandletOppgave.oppgaveId,
                    ansvarligIdent = beslutter.navIdent,
                    utførtAv = beslutter,
                ),
            )
        }
    }

    @Test
    fun `Skal ferdigstille med brev i ny løsning fra UnderKontroll`() {
        val beslutter = Saksbehandler("Z080808", emptySet(), setOf(BESLUTTER))
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
    fun `Skal ikke kunne ferdigstille med brev i Arena fra UnderKontroll`() {
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
                                    id = UUIDv7.ny().toString(),
                                    behandlingId = UUIDv7.ny(),
                                    behandletHendelseType = "Søknad",
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
                id = UUIDv7.ny().toString(),
                behandletHendelseType = "Søknad",
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

        oppgave.sendTilKontroll(SendTilKontrollHendelse(oppgaveId = oppgaveId, utførtAv = saksbehandler2))
        oppgave.sisteSaksbehandler() shouldBe saksbehandler2.navIdent

        val beslutter1 = Saksbehandler("beslutter 1", emptySet(), setOf(BESLUTTER))
        oppgave.tildel(
            SettOppgaveAnsvarHendelse(
                oppgaveId = oppgave.oppgaveId,
                ansvarligIdent = beslutter1.navIdent,
                utførtAv = beslutter1,
            ),
        )
        oppgave.sisteBeslutter() shouldBe beslutter1.navIdent

        oppgave.returnerTilSaksbehandling(ReturnerTilSaksbehandlingHendelse(oppgaveId, beslutter1))

        oppgave.sisteBeslutter() shouldBe beslutter1.navIdent
        oppgave.sisteSaksbehandler() shouldBe saksbehandler2.navIdent

        val beslutter2 = Saksbehandler("beslutter 2", emptySet(), setOf(BESLUTTER))
        oppgave.sendTilKontroll(SendTilKontrollHendelse(oppgaveId = oppgave.oppgaveId, utførtAv = saksbehandler2))

        oppgave.sisteBeslutter() shouldBe beslutter1.navIdent
        oppgave.sisteSaksbehandler() shouldBe saksbehandler2.navIdent
        oppgave.tilstand().type shouldBe UNDER_KONTROLL

        oppgave.fjernAnsvar(FjernOppgaveAnsvarHendelse(oppgaveId = oppgaveId, utførtAv = beslutter1))
        oppgave.tildel(
            SettOppgaveAnsvarHendelse(
                oppgaveId = oppgaveId,
                ansvarligIdent = beslutter2.navIdent,
                utførtAv = beslutter2,
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
}
