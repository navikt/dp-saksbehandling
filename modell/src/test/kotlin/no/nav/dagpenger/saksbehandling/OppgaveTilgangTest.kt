package no.nav.dagpenger.saksbehandling

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.FORTROLIG
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.STRENGT_FORTROLIG
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.ManglendeTilgang
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_KONTROLL
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_KONTROLL
import no.nav.dagpenger.saksbehandling.OppgaveTestHelper.lagOppgave
import no.nav.dagpenger.saksbehandling.OppgaveTestHelper.lagSaksbehandler
import no.nav.dagpenger.saksbehandling.TilgangType.BESLUTTER
import no.nav.dagpenger.saksbehandling.TilgangType.EGNE_ANSATTE
import no.nav.dagpenger.saksbehandling.TilgangType.FORTROLIG_ADRESSE
import no.nav.dagpenger.saksbehandling.TilgangType.SAKSBEHANDLER
import no.nav.dagpenger.saksbehandling.TilgangType.STRENGT_FORTROLIG_ADRESSE
import no.nav.dagpenger.saksbehandling.TilgangType.STRENGT_FORTROLIG_ADRESSE_UTLAND
import no.nav.dagpenger.saksbehandling.hendelser.GodkjennBehandlingMedBrevIArena
import no.nav.dagpenger.saksbehandling.hendelser.GodkjentBehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ReturnerTilSaksbehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SendTilKontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.UtsettOppgaveHendelse
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate
import java.util.stream.Stream

class OppgaveTilgangTest {
    private val saksbehandlerUtenEkstraTilganger =
        Saksbehandler(
            navIdent = "saksbehandler",
            grupper = setOf(),
            tilganger = setOf(SAKSBEHANDLER),
        )

    val saksbehandlerMedTilgangTilEgneAnsatte =
        Saksbehandler(
            navIdent = "saksbehandlerMedTilgangTilEgneAnsatte",
            grupper = setOf(),
            tilganger = setOf(SAKSBEHANDLER, EGNE_ANSATTE),
        )

    companion object {
        @JvmStatic
        private fun adressebeskyttelseTester(): Stream<Arguments> {
            return Stream.of(
                // oppgavens gradering, saksbehandlers tilgang, forventet tilgang
                Arguments.of(UGRADERT, SAKSBEHANDLER, true),
                Arguments.of(FORTROLIG, SAKSBEHANDLER, false),
                Arguments.of(STRENGT_FORTROLIG, SAKSBEHANDLER, false),
                Arguments.of(STRENGT_FORTROLIG_UTLAND, SAKSBEHANDLER, false),
                Arguments.of(UGRADERT, FORTROLIG_ADRESSE, true),
                Arguments.of(FORTROLIG, FORTROLIG_ADRESSE, true),
                Arguments.of(STRENGT_FORTROLIG, FORTROLIG_ADRESSE, false),
                Arguments.of(STRENGT_FORTROLIG_UTLAND, FORTROLIG_ADRESSE, false),
                Arguments.of(UGRADERT, STRENGT_FORTROLIG_ADRESSE, true),
                Arguments.of(FORTROLIG, STRENGT_FORTROLIG_ADRESSE, false),
                Arguments.of(STRENGT_FORTROLIG, STRENGT_FORTROLIG_ADRESSE, true),
                Arguments.of(STRENGT_FORTROLIG_UTLAND, STRENGT_FORTROLIG_ADRESSE, false),
                Arguments.of(UGRADERT, STRENGT_FORTROLIG_ADRESSE_UTLAND, true),
                Arguments.of(FORTROLIG, STRENGT_FORTROLIG_ADRESSE_UTLAND, false),
                Arguments.of(STRENGT_FORTROLIG, STRENGT_FORTROLIG_ADRESSE_UTLAND, false),
                Arguments.of(STRENGT_FORTROLIG_UTLAND, STRENGT_FORTROLIG_ADRESSE_UTLAND, true),
            )
        }
    }

    @Test
    fun `Egne ansatte tilganger ved tildeling av totrinnskontroll oppgaver`() {
        val egneAnsatteOppgave =
            lagOppgave(
                KLAR_TIL_KONTROLL,
                skjermesSomEgneAnsatte = true,
            )
        val beslutterUtenTilgangTilEgneAnsatte =
            Saksbehandler(
                navIdent = "saksbehandler",
                grupper = setOf(),
                tilganger = setOf(BESLUTTER),
            )
        val beslutterMedtilgangTilEgneAnsatte =
            Saksbehandler(
                navIdent = "saksbehandler",
                grupper = setOf(),
                tilganger = setOf(BESLUTTER, EGNE_ANSATTE),
            )

        shouldThrow<ManglendeTilgang> {
            egneAnsatteOppgave.tildel(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = egneAnsatteOppgave.oppgaveId,
                    ansvarligIdent = beslutterUtenTilgangTilEgneAnsatte.navIdent,
                    utførtAv = beslutterUtenTilgangTilEgneAnsatte,
                ),
            )
        }
        shouldNotThrow<ManglendeTilgang> {
            egneAnsatteOppgave.tildel(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = egneAnsatteOppgave.oppgaveId,
                    ansvarligIdent = beslutterMedtilgangTilEgneAnsatte.navIdent,
                    utførtAv = beslutterMedtilgangTilEgneAnsatte,
                ),
            )
        }
    }

    @ParameterizedTest
    @MethodSource("adressebeskyttelseTester")
    fun `Adressebeskyttelse tilganger ved tildeling av totrinnskontroll oppgave`(
        adressebeskyttelseGradering: AdressebeskyttelseGradering,
        saksbehandlerTilgang: TilgangType,
        forventetTilgang: Boolean,
    ) {
        val oppgave =
            lagOppgave(
                tilstandType = KLAR_TIL_KONTROLL,
                adressebeskyttelseGradering = adressebeskyttelseGradering,
            )
        val saksbehandler =
            Saksbehandler(
                navIdent = "saksbehandler",
                grupper = setOf(),
                tilganger = setOf(saksbehandlerTilgang, BESLUTTER),
            )

        if (forventetTilgang) {
            shouldNotThrow<ManglendeTilgang> {
                oppgave.tildel(
                    SettOppgaveAnsvarHendelse(
                        oppgaveId = oppgave.oppgaveId,
                        ansvarligIdent = saksbehandler.navIdent,
                        utførtAv = saksbehandler,
                    ),
                )
            }
        } else {
            shouldThrow<ManglendeTilgang> {
                oppgave.tildel(
                    SettOppgaveAnsvarHendelse(
                        oppgaveId = oppgave.oppgaveId,
                        ansvarligIdent = saksbehandler.navIdent,
                        utførtAv = saksbehandler,
                    ),
                )
            }
        }
    }

    @ParameterizedTest
    @MethodSource("adressebeskyttelseTester")
    fun `Adressebeskyttelse tilganger ved henting av oppgave`(
        adressebeskyttelseGradering: AdressebeskyttelseGradering,
        saksbehandlerTilgang: TilgangType,
        forventetTilgang: Boolean,
    ) {
        val oppgave = lagOppgave(adressebeskyttelseGradering = adressebeskyttelseGradering)
        val saksbehandler = lagSaksbehandler(saksbehandlerTilgang)

        if (forventetTilgang) {
            shouldNotThrow<ManglendeTilgang> {
                oppgave.tildel(
                    SettOppgaveAnsvarHendelse(
                        oppgaveId = oppgave.oppgaveId,
                        ansvarligIdent = saksbehandler.navIdent,
                        utførtAv = saksbehandler,
                    ),
                )
            }
        } else {
            shouldThrow<ManglendeTilgang> {
                oppgave.tildel(
                    SettOppgaveAnsvarHendelse(
                        oppgaveId = oppgave.oppgaveId,
                        ansvarligIdent = saksbehandler.navIdent,
                        utførtAv = saksbehandler,
                    ),
                )
            }
        }
    }

    @Test
    fun `Egne ansatte tilganger ved tildeling av oppgaver`() {
        val egneAnsatteOppgave =
            lagOppgave(
                KLAR_TIL_BEHANDLING,
                skjermesSomEgneAnsatte = true,
            )

        shouldThrow<ManglendeTilgang> {
            egneAnsatteOppgave.tildel(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = egneAnsatteOppgave.oppgaveId,
                    ansvarligIdent = saksbehandlerUtenEkstraTilganger.navIdent,
                    utførtAv = saksbehandlerUtenEkstraTilganger,
                ),
            )
        }
        shouldNotThrow<ManglendeTilgang> {
            egneAnsatteOppgave.tildel(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = egneAnsatteOppgave.oppgaveId,
                    ansvarligIdent = saksbehandlerMedTilgangTilEgneAnsatte.navIdent,
                    utførtAv = saksbehandlerMedTilgangTilEgneAnsatte,
                ),
            )
        }
    }

    @ParameterizedTest
    @MethodSource("adressebeskyttelseTester")
    fun `Adressebeskyttelse tilganger ved tildeling av oppgaver`(
        adressebeskyttelseGradering: AdressebeskyttelseGradering,
        saksbehandlerTilgang: TilgangType,
        forventetTilgang: Boolean,
    ) {
        val oppgave = lagOppgave(adressebeskyttelseGradering = adressebeskyttelseGradering)
        val saksbehandler = lagSaksbehandler(saksbehandlerTilgang)

        if (forventetTilgang) {
            shouldNotThrow<ManglendeTilgang> {
                oppgave.tildel(
                    SettOppgaveAnsvarHendelse(
                        oppgaveId = oppgave.oppgaveId,
                        ansvarligIdent = saksbehandler.navIdent,
                        utførtAv = saksbehandler,
                    ),
                )
            }
        } else {
            shouldThrow<ManglendeTilgang> {
                oppgave.tildel(
                    SettOppgaveAnsvarHendelse(
                        oppgaveId = oppgave.oppgaveId,
                        ansvarligIdent = saksbehandler.navIdent,
                        utførtAv = saksbehandler,
                    ),
                )
            }
        }
    }

    @Test
    fun `Oppgave med egne ansatte skjerming og adressebeskyttelse`() {
        val oppgave =
            lagOppgave(
                skjermesSomEgneAnsatte = true,
                adressebeskyttelseGradering = FORTROLIG,
            )
        val saksbehandlerMedEgneAnsatteTilgang = lagSaksbehandler(saksbehandlerTilgang = EGNE_ANSATTE)
        val saksbehandlerMedFortroligTilgang = lagSaksbehandler(saksbehandlerTilgang = FORTROLIG_ADRESSE)
        val saksbehandlerMedFortroligOgEgneAnsatteTilgang =
            Saksbehandler(
                navIdent = "saksbehandler",
                grupper = setOf(),
                tilganger = setOf(EGNE_ANSATTE, FORTROLIG_ADRESSE),
            )

        shouldThrow<ManglendeTilgang> {
            oppgave.tildel(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    ansvarligIdent = saksbehandlerMedEgneAnsatteTilgang.navIdent,
                    utførtAv = saksbehandlerMedEgneAnsatteTilgang,
                ),
            )
            oppgave.tildel(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    ansvarligIdent = saksbehandlerMedFortroligTilgang.navIdent,
                    utførtAv = saksbehandlerMedFortroligTilgang,
                ),
            )
        }

        shouldNotThrow<ManglendeTilgang> {
            oppgave.tildel(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    ansvarligIdent = saksbehandlerMedFortroligOgEgneAnsatteTilgang.navIdent,
                    utførtAv = saksbehandlerMedFortroligOgEgneAnsatteTilgang,
                ),
            )
        }
    }

    @Test
    fun `Egne ansatte tilganger ved utsetting av oppgaver`() {
        val egneAnsatteOppgave =
            lagOppgave(
                UNDER_BEHANDLING,
                behandler = saksbehandlerUtenEkstraTilganger,
                skjermesSomEgneAnsatte = true,
            )

        shouldThrow<ManglendeTilgang> {
            egneAnsatteOppgave.utsett(
                UtsettOppgaveHendelse(
                    oppgaveId = egneAnsatteOppgave.oppgaveId,
                    utførtAv = saksbehandlerUtenEkstraTilganger,
                    navIdent = saksbehandlerUtenEkstraTilganger.navIdent,
                    utsattTil = LocalDate.MAX,
                    beholdOppgave = true,
                ),
            )
        }

        shouldNotThrow<ManglendeTilgang> {
            egneAnsatteOppgave.utsett(
                UtsettOppgaveHendelse(
                    oppgaveId = egneAnsatteOppgave.oppgaveId,
                    utførtAv = saksbehandlerMedTilgangTilEgneAnsatte,
                    navIdent = saksbehandlerMedTilgangTilEgneAnsatte.navIdent,
                    utsattTil = LocalDate.MAX,
                    beholdOppgave = true,
                ),
            )
        }
    }

    @ParameterizedTest
    @MethodSource("adressebeskyttelseTester")
    fun `Adressebeskyttelse tilganger ved utsetting av oppgaver`(
        adressebeskyttelseGradering: AdressebeskyttelseGradering,
        saksbehandlerTilgang: TilgangType,
        forventetTilgang: Boolean,
    ) {
        val oppgave =
            lagOppgave(
                tilstandType = UNDER_BEHANDLING,
                adressebeskyttelseGradering = adressebeskyttelseGradering,
            )
        val saksbehandler = lagSaksbehandler(saksbehandlerTilgang)

        if (forventetTilgang) {
            shouldNotThrow<ManglendeTilgang> {
                oppgave.utsett(
                    UtsettOppgaveHendelse(
                        oppgaveId = oppgave.oppgaveId,
                        utførtAv = saksbehandler,
                        navIdent = saksbehandler.navIdent,
                        utsattTil = LocalDate.MAX,
                        beholdOppgave = true,
                    ),
                )
            }
        } else {
            shouldThrow<ManglendeTilgang> {
                oppgave.utsett(
                    UtsettOppgaveHendelse(
                        oppgaveId = oppgave.oppgaveId,
                        utførtAv = saksbehandler,
                        navIdent = saksbehandler.navIdent,
                        utsattTil = LocalDate.MAX,
                        beholdOppgave = true,
                    ),
                )
            }
        }
    }

    @ParameterizedTest
    @MethodSource("adressebeskyttelseTester")
    fun `Adressebeskyttelse tilganger ved sending av oppgave til kontroll`(
        adressebeskyttelseGradering: AdressebeskyttelseGradering,
        saksbehandlerTilgang: TilgangType,
        forventetTilgang: Boolean,
    ) {
        val oppgave =
            lagOppgave(
                tilstandType = UNDER_BEHANDLING,
                adressebeskyttelseGradering = adressebeskyttelseGradering,
            )
        val saksbehandler = lagSaksbehandler(saksbehandlerTilgang)
        oppgave.behandlerIdent = saksbehandler.navIdent

        if (forventetTilgang) {
            shouldNotThrow<ManglendeTilgang> {
                oppgave.sendTilKontroll(
                    SendTilKontrollHendelse(
                        oppgaveId = oppgave.oppgaveId,
                        utførtAv = saksbehandler,
                    ),
                )
            }
        } else {
            shouldThrow<ManglendeTilgang> {
                oppgave.sendTilKontroll(
                    SendTilKontrollHendelse(
                        oppgaveId = oppgave.oppgaveId,
                        utførtAv = saksbehandler,
                    ),
                )
            }
        }
    }

    @Test
    fun `Egne ansatte tilganger ved sending av oppgave til kontroll`() {
        shouldThrow<ManglendeTilgang> {
            lagOppgave(
                tilstandType = UNDER_BEHANDLING,
                behandler = saksbehandlerUtenEkstraTilganger,
                skjermesSomEgneAnsatte = true,
            ).let {
                it.sendTilKontroll(
                    SendTilKontrollHendelse(
                        oppgaveId = it.oppgaveId,
                        utførtAv = saksbehandlerUtenEkstraTilganger,
                    ),
                )
            }
        }

        shouldNotThrow<ManglendeTilgang> {
            lagOppgave(
                tilstandType = UNDER_BEHANDLING,
                behandler = saksbehandlerMedTilgangTilEgneAnsatte,
                skjermesSomEgneAnsatte = true,
            ).let {
                it.sendTilKontroll(
                    SendTilKontrollHendelse(
                        oppgaveId = it.oppgaveId,
                        utførtAv = saksbehandlerMedTilgangTilEgneAnsatte,
                    ),
                )
            }
        }
    }

    @Test
    fun `Ferdigstilling av oppgave under behandling med brev fra Arena krever at utførende saksbehandler også eier oppgaven`() {
        val oppgave = lagOppgave(tilstandType = UNDER_BEHANDLING, behandler = saksbehandlerUtenEkstraTilganger)
        shouldThrow<ManglendeTilgang> {
            oppgave.ferdigstill(
                GodkjennBehandlingMedBrevIArena(
                    oppgaveId = oppgave.oppgaveId,
                    utførtAv = saksbehandlerMedTilgangTilEgneAnsatte,
                ),
            )
        }

        shouldNotThrow<ManglendeTilgang> {
            oppgave.ferdigstill(
                GodkjennBehandlingMedBrevIArena(
                    oppgaveId = oppgave.oppgaveId,
                    utførtAv = saksbehandlerUtenEkstraTilganger,
                ),
            )
        }
    }

    @Test
    fun `Ferdigstilling av oppgave under behandling krever at utførende saksbehandler også eier oppgaven`() {
        val oppgave = lagOppgave(tilstandType = UNDER_BEHANDLING, behandler = saksbehandlerUtenEkstraTilganger)
        shouldThrow<ManglendeTilgang> {
            oppgave.ferdigstill(
                GodkjentBehandlingHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    meldingOmVedtak = "<HTML>en melding</HTML>",
                    utførtAv = saksbehandlerMedTilgangTilEgneAnsatte,
                ),
            )
        }

        shouldNotThrow<ManglendeTilgang> {
            oppgave.ferdigstill(
                GodkjentBehandlingHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    meldingOmVedtak = "<HTML>en melding</HTML>",
                    utførtAv = saksbehandlerUtenEkstraTilganger,
                ),
            )
        }
    }

    @Test
    fun `Ferdigstilling av oppgave under kontroll krever at utførende eier oppgaven og er beslutter`() {
        val beslutterSomEierOppgaven = Saksbehandler("eier", setOf(), setOf(BESLUTTER))
        val saksbehandler = Saksbehandler("saksbehandler", setOf(), setOf(SAKSBEHANDLER))

        val oppgave = lagOppgave(tilstandType = UNDER_KONTROLL, behandler = beslutterSomEierOppgaven)
        shouldThrow<ManglendeTilgang> {
            oppgave.ferdigstill(
                GodkjentBehandlingHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    meldingOmVedtak = "<HTML>en melding</HTML>",
                    utførtAv = saksbehandler,
                ),
            )
        }

        val enAnnenBeslutter = Saksbehandler("beslutter 2", setOf(), setOf(BESLUTTER))
        shouldThrow<ManglendeTilgang> {
            oppgave.ferdigstill(
                GodkjentBehandlingHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    meldingOmVedtak = "<HTML>en melding</HTML>",
                    utførtAv = enAnnenBeslutter,
                ),
            )
        }

        val saksbehandlerSomVarBeslutter =
            Saksbehandler("saksbehandler som var beslutter", setOf(), setOf(SAKSBEHANDLER))
        val oppgaveUnderKontrollUtenBeslutter =
            lagOppgave(tilstandType = UNDER_KONTROLL, behandler = saksbehandlerSomVarBeslutter)
        shouldThrow<ManglendeTilgang> {
            oppgaveUnderKontrollUtenBeslutter.ferdigstill(
                GodkjentBehandlingHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    meldingOmVedtak = "<HTML>en melding</HTML>",
                    utførtAv = saksbehandlerSomVarBeslutter,
                ),
            )
        }

        shouldNotThrow<ManglendeTilgang> {
            oppgave.ferdigstill(
                GodkjentBehandlingHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    meldingOmVedtak = "<HTML>en melding</HTML>",
                    utførtAv = beslutterSomEierOppgaven,
                ),
            )
        }
    }

    @Test
    fun `Oppgave klar til kontroll kan ikke tildeles samme behandler som saksbehandlet, selv om hen er beslutter`() {
        val beslutterSomSaksbehandlet =
            Saksbehandler("beslutterSomSaksbehandlet", setOf(), setOf(SAKSBEHANDLER, BESLUTTER))
        val oppgave = lagOppgave(tilstandType = KLAR_TIL_KONTROLL)
        oppgave.tilstandslogg.leggTil(
            UNDER_BEHANDLING,
            SettOppgaveAnsvarHendelse(
                oppgaveId = oppgave.oppgaveId,
                ansvarligIdent = beslutterSomSaksbehandlet.navIdent,
                utførtAv = beslutterSomSaksbehandlet,
            ),
        )

        shouldThrow<ManglendeTilgang> {
            oppgave.tildel(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    utførtAv = beslutterSomSaksbehandlet,
                    ansvarligIdent = beslutterSomSaksbehandlet.navIdent,
                ),
            )
        }

        val enAnnenBeslutter = Saksbehandler("enAnnenBeslutter", setOf(), setOf(SAKSBEHANDLER, BESLUTTER))
        shouldNotThrow<ManglendeTilgang> {
            oppgave.tildel(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    utførtAv = enAnnenBeslutter,
                    ansvarligIdent = enAnnenBeslutter.navIdent,
                ),
            )
        }
    }

    @Test
    fun `Oppgave under kontroll kan ikke ferdigstilles av samme behandler som saksbehandlet, selv om hen er beslutter`() {
        val beslutterSomSaksbehandlet = Saksbehandler("eier", setOf(), setOf(SAKSBEHANDLER, BESLUTTER))
        val oppgave1 = lagOppgave(tilstandType = UNDER_KONTROLL, behandler = beslutterSomSaksbehandlet)
        oppgave1.tilstandslogg.leggTil(
            UNDER_BEHANDLING,
            SettOppgaveAnsvarHendelse(
                oppgaveId = oppgave1.oppgaveId,
                ansvarligIdent = beslutterSomSaksbehandlet.navIdent,
                utførtAv = beslutterSomSaksbehandlet,
            ),
        )
        shouldThrow<ManglendeTilgang> {
            oppgave1.ferdigstill(
                GodkjentBehandlingHendelse(
                    oppgaveId = oppgave1.oppgaveId,
                    meldingOmVedtak = "<HTML>en melding</HTML>",
                    utførtAv = beslutterSomSaksbehandlet,
                ),
            )
        }

        val enAnnenBeslutter = Saksbehandler("enAnnenBeslutter", setOf(), setOf(SAKSBEHANDLER, BESLUTTER))
        val oppgave2 = lagOppgave(tilstandType = UNDER_KONTROLL, behandler = enAnnenBeslutter)
        oppgave2.tilstandslogg.leggTil(
            UNDER_BEHANDLING,
            SettOppgaveAnsvarHendelse(
                oppgaveId = oppgave2.oppgaveId,
                ansvarligIdent = beslutterSomSaksbehandlet.navIdent,
                utførtAv = beslutterSomSaksbehandlet,
            ),
        )
        shouldNotThrow<ManglendeTilgang> {
            oppgave2.ferdigstill(
                GodkjentBehandlingHendelse(
                    oppgaveId = oppgave2.oppgaveId,
                    meldingOmVedtak = "<HTML>en melding</HTML>",
                    utførtAv = enAnnenBeslutter,
                ),
            )
        }
    }

    @Test
    fun `Oppgave under kontroll kan ikke retureres til saksbehandling av samme behandler som saksbehandlet`() {
        val beslutterSomSaksbehandlet = Saksbehandler("eier", setOf(), setOf(SAKSBEHANDLER, BESLUTTER))
        val oppgave1 = lagOppgave(tilstandType = UNDER_KONTROLL, behandler = beslutterSomSaksbehandlet)
        oppgave1.tilstandslogg.leggTil(
            UNDER_BEHANDLING,
            SettOppgaveAnsvarHendelse(
                oppgaveId = oppgave1.oppgaveId,
                ansvarligIdent = beslutterSomSaksbehandlet.navIdent,
                utførtAv = beslutterSomSaksbehandlet,
            ),
        )
        shouldThrow<ManglendeTilgang> {
            oppgave1.returnerTilSaksbehandling(
                ReturnerTilSaksbehandlingHendelse(
                    oppgaveId = oppgave1.oppgaveId,
                    utførtAv = beslutterSomSaksbehandlet,
                ),
            )
        }

        val enAnnenBeslutter = Saksbehandler("beslutter 2", setOf(), setOf(SAKSBEHANDLER, BESLUTTER))
        val oppgave2 = lagOppgave(tilstandType = UNDER_KONTROLL, behandler = enAnnenBeslutter)
        oppgave2.tilstandslogg.leggTil(
            UNDER_BEHANDLING,
            SettOppgaveAnsvarHendelse(
                oppgaveId = oppgave2.oppgaveId,
                ansvarligIdent = beslutterSomSaksbehandlet.navIdent,
                utførtAv = beslutterSomSaksbehandlet,
            ),
        )
        shouldNotThrow<ManglendeTilgang> {
            oppgave2.returnerTilSaksbehandling(
                ReturnerTilSaksbehandlingHendelse(
                    oppgaveId = oppgave2.oppgaveId,
                    utførtAv = enAnnenBeslutter,
                ),
            )
        }
    }

    @ParameterizedTest
    @MethodSource("adressebeskyttelseTester")
    fun `Saksbehandler må ha tilgang for å ferdigstille en oppgave i nytt system med adressebeskyttelse`(
        adressebeskyttelseGradering: AdressebeskyttelseGradering,
        saksbehandlerTilgang: TilgangType,
        forventetTilgang: Boolean,
    ) {
        val saksbehandler = lagSaksbehandler(saksbehandlerTilgang)
        val oppgave =
            lagOppgave(
                adressebeskyttelseGradering = adressebeskyttelseGradering,
                tilstandType = UNDER_BEHANDLING,
                behandler = saksbehandler,
            )

        if (forventetTilgang) {
            shouldNotThrow<ManglendeTilgang> {
                oppgave.ferdigstill(
                    GodkjentBehandlingHendelse(
                        oppgaveId = oppgave.oppgaveId,
                        meldingOmVedtak = "<HTML>en melding</HTML>",
                        utførtAv = saksbehandler,
                    ),
                )
            }
        } else {
            shouldThrow<ManglendeTilgang> {
                oppgave.ferdigstill(
                    GodkjentBehandlingHendelse(
                        oppgaveId = oppgave.oppgaveId,
                        meldingOmVedtak = "<HTML>en melding</HTML>",
                        utførtAv = saksbehandler,
                    ),
                )
            }
        }
    }

    @Test
    fun `Saksbehandler må ha tilgang til egne ansatte for å ferdigstill en oppgave for egne ansatte`() {
        val saksbehandlerMedEgneAnsatteTilgang = lagSaksbehandler(saksbehandlerTilgang = EGNE_ANSATTE)
        val saksbehandler = lagSaksbehandler(saksbehandlerTilgang = SAKSBEHANDLER)

        shouldThrow<ManglendeTilgang> {
            val oppgave =
                lagOppgave(
                    tilstandType = UNDER_BEHANDLING,
                    skjermesSomEgneAnsatte = true,
                    behandler = saksbehandler,
                )
            oppgave.ferdigstill(
                GodkjentBehandlingHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    meldingOmVedtak = "<HTML>en melding</HTML>",
                    utførtAv = saksbehandler,
                ),
            )
        }

        shouldNotThrow<ManglendeTilgang> {
            val oppgave =
                lagOppgave(
                    tilstandType = UNDER_BEHANDLING,
                    skjermesSomEgneAnsatte = true,
                    behandler = saksbehandlerMedEgneAnsatteTilgang,
                )
            oppgave.ferdigstill(
                GodkjentBehandlingHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    meldingOmVedtak = "<HTML>en melding</HTML>",
                    utførtAv = saksbehandlerMedEgneAnsatteTilgang,
                ),
            )
        }

        shouldThrow<ManglendeTilgang> {
            val oppgave =
                lagOppgave(
                    tilstandType = UNDER_BEHANDLING,
                    skjermesSomEgneAnsatte = true,
                    behandler = saksbehandler,
                )
            oppgave.ferdigstill(
                GodkjennBehandlingMedBrevIArena(
                    oppgaveId = oppgave.oppgaveId,
                    utførtAv = saksbehandler,
                ),
            )
        }

        shouldNotThrow<ManglendeTilgang> {
            val oppgave =
                lagOppgave(
                    tilstandType = UNDER_BEHANDLING,
                    skjermesSomEgneAnsatte = true,
                    behandler = saksbehandlerMedEgneAnsatteTilgang,
                )
            oppgave.ferdigstill(
                GodkjennBehandlingMedBrevIArena(
                    oppgaveId = oppgave.oppgaveId,
                    utførtAv = saksbehandlerMedEgneAnsatteTilgang,
                ),
            )
        }
    }

    @ParameterizedTest
    @MethodSource("adressebeskyttelseTester")
    fun `Saksbehandler må ha tilgang for å ferdigstille en oppgave med brev i Arena med adressebeskyttelse`(
        adressebeskyttelseGradering: AdressebeskyttelseGradering,
        saksbehandlerTilgang: TilgangType,
        forventetTilgang: Boolean,
    ) {
        val saksbehandler = lagSaksbehandler(saksbehandlerTilgang)
        val oppgave =
            lagOppgave(
                adressebeskyttelseGradering = adressebeskyttelseGradering,
                tilstandType = UNDER_BEHANDLING,
                behandler = saksbehandler,
            )

        if (forventetTilgang) {
            shouldNotThrow<ManglendeTilgang> {
                oppgave.ferdigstill(
                    GodkjennBehandlingMedBrevIArena(
                        oppgaveId = oppgave.oppgaveId,
                        utførtAv = saksbehandler,
                    ),
                )
            }
        } else {
            shouldThrow<ManglendeTilgang> {
                oppgave.ferdigstill(
                    GodkjennBehandlingMedBrevIArena(
                        oppgaveId = oppgave.oppgaveId,
                        utførtAv = saksbehandler,
                    ),
                )
            }
        }
    }
}
