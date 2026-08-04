package no.nav.dagpenger.saksbehandling

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.FORTROLIG
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.STRENGT_FORTROLIG
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.Emneknagg.AvbrytBehandling
import no.nav.dagpenger.saksbehandling.ModellTestHelper.lagOppgave
import no.nav.dagpenger.saksbehandling.ModellTestHelper.lagSaksbehandler
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_KONTROLL
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_BEHANDLING
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.UNDER_KONTROLL
import no.nav.dagpenger.saksbehandling.TilgangType.BESLUTTER
import no.nav.dagpenger.saksbehandling.TilgangType.EGNE_ANSATTE
import no.nav.dagpenger.saksbehandling.TilgangType.FORTROLIG_ADRESSE
import no.nav.dagpenger.saksbehandling.TilgangType.SAKSBEHANDLER
import no.nav.dagpenger.saksbehandling.TilgangType.STRENGT_FORTROLIG_ADRESSE
import no.nav.dagpenger.saksbehandling.TilgangType.STRENGT_FORTROLIG_ADRESSE_UTLAND
import no.nav.dagpenger.saksbehandling.hendelser.AvbrytOppgaveHendelse
import no.nav.dagpenger.saksbehandling.hendelser.GodkjentBehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ReturnerTilSaksbehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SendTilKontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.UtsettOppgaveHendelse
import no.nav.dagpenger.saksbehandling.tilgangsstyring.ManglendeTilgang
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
        private fun adressebeskyttelseTester(): Stream<Arguments> =
            Stream.of(
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

    @Test
    fun `Inhabilitet tilganger ved tildeling av oppgaver`() {
        val habilSaksbehandler = lagSaksbehandler()
        val inhabilSaksbehandler = lagSaksbehandler(navIdent = "inhabil123")
        val oppgave = lagOppgave(inhabileSaksbehandlerIdenter = listOf(inhabilSaksbehandler.navIdent))

        shouldNotThrow<Inhabil> {
            oppgave.tildel(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    ansvarligIdent = habilSaksbehandler.navIdent,
                    utførtAv = habilSaksbehandler,
                ),
            )
        }
        shouldThrow<Inhabil> {
            oppgave.tildel(
                SettOppgaveAnsvarHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    ansvarligIdent = inhabilSaksbehandler.navIdent,
                    utførtAv = inhabilSaksbehandler,
                ),
            )
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
        val saksbehandler = lagSaksbehandler(saksbehandlerTilgang = saksbehandlerTilgang)

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
        val saksbehandler = lagSaksbehandler(saksbehandlerTilgang = saksbehandlerTilgang)

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
                saksbehandler = saksbehandlerUtenEkstraTilganger,
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
        val saksbehandler = lagSaksbehandler(saksbehandlerTilgang = saksbehandlerTilgang)

        if (forventetTilgang) {
            shouldNotThrow<ManglendeTilgang> {
                oppgave.utsett(
                    UtsettOppgaveHendelse(
                        oppgaveId = oppgave.oppgaveId,
                        utførtAv = saksbehandler,
                        navIdent = saksbehandler.navIdent,
                        utsattTil = LocalDate.MAX,
                        beholdOppgave = true,
                        årsak = Emneknagg.PåVent.AVVENT_ANNET,
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
        val saksbehandler = lagSaksbehandler(saksbehandlerTilgang = saksbehandlerTilgang)
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
                saksbehandler = saksbehandlerUtenEkstraTilganger,
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
                saksbehandler = saksbehandlerMedTilgangTilEgneAnsatte,
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

    @ParameterizedTest
    @MethodSource("adressebeskyttelseTester")
    fun `Adressebeskyttelse tilganger ved avbryting av oppgave`(
        adressebeskyttelseGradering: AdressebeskyttelseGradering,
        saksbehandlerTilgang: TilgangType,
        forventetTilgang: Boolean,
    ) {
        val saksbehandler = lagSaksbehandler(saksbehandlerTilgang = saksbehandlerTilgang)
        val oppgave =
            lagOppgave(
                tilstandType = UNDER_BEHANDLING,
                adressebeskyttelseGradering = adressebeskyttelseGradering,
                saksbehandler = saksbehandler,
            )

        if (forventetTilgang) {
            shouldNotThrow<ManglendeTilgang> {
                oppgave.avbryt(
                    AvbrytOppgaveHendelse(
                        oppgaveId = oppgave.oppgaveId,
                        årsak = AvbrytBehandling.AVBRUTT_ANNET,
                        navIdent = saksbehandler.navIdent,
                        utførtAv = saksbehandler,
                    ),
                )
            }
        } else {
            shouldThrow<ManglendeTilgang> {
                oppgave.avbryt(
                    AvbrytOppgaveHendelse(
                        oppgaveId = oppgave.oppgaveId,
                        årsak = AvbrytBehandling.AVBRUTT_ANNET,
                        navIdent = saksbehandler.navIdent,
                        utførtAv = saksbehandler,
                    ),
                )
            }
        }
    }

    @Test
    fun `Egne ansatte tilganger ved avbryting av oppgave`() {
        shouldThrow<ManglendeTilgang> {
            lagOppgave(
                tilstandType = UNDER_BEHANDLING,
                saksbehandler = saksbehandlerUtenEkstraTilganger,
                skjermesSomEgneAnsatte = true,
            ).let { oppgave ->
                oppgave.avbryt(
                    AvbrytOppgaveHendelse(
                        oppgaveId = oppgave.oppgaveId,
                        årsak = AvbrytBehandling.AVBRUTT_ANNET,
                        navIdent = saksbehandlerUtenEkstraTilganger.navIdent,
                        utførtAv = saksbehandlerUtenEkstraTilganger,
                    ),
                )
            }
        }

        shouldNotThrow<ManglendeTilgang> {
            lagOppgave(
                tilstandType = UNDER_BEHANDLING,
                saksbehandler = saksbehandlerMedTilgangTilEgneAnsatte,
                skjermesSomEgneAnsatte = true,
            ).let { oppgave ->
                oppgave.avbryt(
                    AvbrytOppgaveHendelse(
                        oppgaveId = oppgave.oppgaveId,
                        årsak = AvbrytBehandling.AVBRUTT_ANNET,
                        navIdent = saksbehandlerMedTilgangTilEgneAnsatte.navIdent,
                        utførtAv = saksbehandlerMedTilgangTilEgneAnsatte,
                    ),
                )
            }
        }
    }

    @Test
    fun `Avbryting av oppgave under behandling krever at utførende saksbehandler også eier oppgaven`() {
        val oppgave = lagOppgave(tilstandType = UNDER_BEHANDLING, saksbehandler = saksbehandlerUtenEkstraTilganger)
        shouldThrow<ManglendeTilgang> {
            oppgave.avbryt(
                AvbrytOppgaveHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    årsak = AvbrytBehandling.AVBRUTT_BEHANDLES_I_ARENA,
                    navIdent = saksbehandlerMedTilgangTilEgneAnsatte.navIdent,
                    utførtAv = saksbehandlerMedTilgangTilEgneAnsatte,
                ),
            )
        }

        shouldNotThrow<ManglendeTilgang> {
            oppgave.avbryt(
                AvbrytOppgaveHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    årsak = AvbrytBehandling.AVBRUTT_BEHANDLES_I_ARENA,
                    navIdent = saksbehandlerUtenEkstraTilganger.navIdent,
                    utførtAv = saksbehandlerUtenEkstraTilganger,
                ),
            )
        }
    }

    @Test
    fun `Ferdigstilling av oppgave under behandling med brev krever at utførende saksbehandler også eier oppgaven`() {
        val oppgave = lagOppgave(tilstandType = UNDER_BEHANDLING, saksbehandler = saksbehandlerUtenEkstraTilganger)
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
    fun `Ferdigstilling av oppgave under kontroll krever at utførende behandler eier oppgaven og er beslutter`() {
        val beslutter = Saksbehandler("eier", setOf(), setOf(BESLUTTER))
        val saksbehandler = Saksbehandler("saksbehandler", setOf(), setOf(SAKSBEHANDLER))
        val oppgave = lagOppgave(tilstandType = UNDER_KONTROLL, saksbehandler = saksbehandler, beslutter = beslutter)

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
            lagOppgave(tilstandType = UNDER_KONTROLL, saksbehandler = saksbehandlerSomVarBeslutter)
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
                    utførtAv = beslutter,
                ),
            )
        }
    }

    @Test
    fun `Oppgave klar til kontroll kan ikke tildeles samme behandler som saksbehandlet, selv om hen er beslutter`() {
        val beslutterSomSaksbehandlet =
            Saksbehandler("beslutterSomSaksbehandlet", setOf(), setOf(SAKSBEHANDLER, BESLUTTER))
        val oppgave = lagOppgave(tilstandType = KLAR_TIL_KONTROLL, saksbehandler = beslutterSomSaksbehandlet)

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
        val beslutterSomSaksbehandlet = Saksbehandler("beslutter", setOf(), setOf(SAKSBEHANDLER, BESLUTTER))
        val enAnnenBeslutter = Saksbehandler("enAnnenBeslutter", setOf(), setOf(SAKSBEHANDLER, BESLUTTER))
        val oppgave = lagOppgave(tilstandType = UNDER_KONTROLL, saksbehandler = beslutterSomSaksbehandlet, beslutter = enAnnenBeslutter)

        shouldThrow<ManglendeTilgang> {
            oppgave.ferdigstill(
                GodkjentBehandlingHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    meldingOmVedtak = "<HTML>en melding</HTML>",
                    utførtAv = beslutterSomSaksbehandlet,
                ),
            )
        }

        shouldNotThrow<ManglendeTilgang> {
            oppgave.ferdigstill(
                GodkjentBehandlingHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    meldingOmVedtak = "<HTML>en melding</HTML>",
                    utførtAv = enAnnenBeslutter,
                ),
            )
        }
    }

    @Test
    fun `Oppgave under kontroll kan ikke retureres til saksbehandling av samme behandler som saksbehandlet`() {
        val beslutter1 = Saksbehandler("beslutter 1", setOf(), setOf(SAKSBEHANDLER, BESLUTTER))
        val oppgave1 = lagOppgave(tilstandType = UNDER_KONTROLL, saksbehandler = beslutter1, beslutter = beslutter1)

        shouldThrow<ManglendeTilgang> {
            oppgave1.returnerTilSaksbehandling(
                ReturnerTilSaksbehandlingHendelse(
                    oppgaveId = oppgave1.oppgaveId,
                    utførtAv = beslutter1,
                ),
            )
        }

        val beslutter2 = Saksbehandler("beslutter 2", setOf(), setOf(SAKSBEHANDLER, BESLUTTER))
        val oppgave2 = lagOppgave(tilstandType = UNDER_KONTROLL, saksbehandler = beslutter1, beslutter = beslutter2)

        shouldNotThrow<ManglendeTilgang> {
            oppgave2.returnerTilSaksbehandling(
                ReturnerTilSaksbehandlingHendelse(
                    oppgaveId = oppgave2.oppgaveId,
                    utførtAv = beslutter2,
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
        val saksbehandler = lagSaksbehandler(saksbehandlerTilgang = saksbehandlerTilgang)
        val oppgave =
            lagOppgave(
                adressebeskyttelseGradering = adressebeskyttelseGradering,
                tilstandType = UNDER_BEHANDLING,
                saksbehandler = saksbehandler,
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
                    saksbehandler = saksbehandler,
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
                    saksbehandler = saksbehandlerMedEgneAnsatteTilgang,
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
                    saksbehandler = saksbehandler,
                )
            oppgave.ferdigstill(
                GodkjentBehandlingHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    utførtAv = saksbehandler,
                    meldingOmVedtak = "<HTML>en melding</HTML>",
                ),
            )
        }

        shouldNotThrow<ManglendeTilgang> {
            val oppgave =
                lagOppgave(
                    tilstandType = UNDER_BEHANDLING,
                    skjermesSomEgneAnsatte = true,
                    saksbehandler = saksbehandlerMedEgneAnsatteTilgang,
                )
            oppgave.ferdigstill(
                GodkjentBehandlingHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    utførtAv = saksbehandlerMedEgneAnsatteTilgang,
                    meldingOmVedtak = "<HTML>en melding</HTML>",
                ),
            )
        }
    }
}
