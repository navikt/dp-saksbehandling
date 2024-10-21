package no.nav.dagpenger.saksbehandling

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldNotThrowAny
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
import no.nav.dagpenger.saksbehandling.hendelser.GodkjentBehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ToTrinnskontrollHendelse
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
            navIdent = "saksbehandler",
            grupper = setOf(),
            tilganger = setOf(SAKSBEHANDLER, EGNE_ANSATTE),
        )

    private val beslutter =
        Saksbehandler(
            navIdent = "beslutter",
            grupper = setOf(),
            tilganger = setOf(BESLUTTER),
        )

    @Test
    fun `Må ha beslutter tilgang for å ferdigstill oppgave med tilstand UNDDER_KONTROLL`() {
        val oppgave =
            lagOppgave(
                tilstandType = UNDER_KONTROLL,
                behandler = saksbehandlerUtenEkstraTilganger,
            )

        shouldThrow<ManglendeTilgang> {
            oppgave.ferdigstill(
                GodkjentBehandlingHendelse(
                    oppgaveId = oppgave.oppgaveId,
                    meldingOmVedtak = "<HTML>en melding</HTML>",
                    utførtAv = saksbehandlerUtenEkstraTilganger,
                ),
            )
        }

        shouldNotThrowAny {
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
    fun `Må ha beslutter tilgang for å ta oppgave med tilstand KLAR_TIL_KONTROLL`() {
        val saksbehandler =
            Saksbehandler(
                navIdent = "saksbehandler",
                grupper = setOf(),
                tilganger = setOf(SAKSBEHANDLER),
            )

        val beslutter =
            Saksbehandler(
                navIdent = "beslutter",
                grupper = setOf(),
                tilganger = setOf(BESLUTTER),
            )

        val oppgave =
            lagOppgave(
                tilstandType = KLAR_TIL_KONTROLL,
            )

        shouldThrow<ManglendeTilgang> {
            oppgave.tildelTotrinnskontroll(
                toTrinnskontrollHendelse =
                    ToTrinnskontrollHendelse(
                        oppgaveId = oppgave.oppgaveId,
                        ansvarligIdent = saksbehandler.navIdent,
                        utførtAv = saksbehandler,
                    ),
            )
        }

        shouldNotThrowAny {
            oppgave.tildelTotrinnskontroll(
                toTrinnskontrollHendelse =
                    ToTrinnskontrollHendelse(
                        oppgaveId = oppgave.oppgaveId,
                        ansvarligIdent = beslutter.navIdent,
                        utførtAv = beslutter,
                    ),
            )
        }
    }

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

    @ParameterizedTest
    @MethodSource("adressebeskyttelseTester")
    fun `Adressebeskyttelse tilgangser ved henting av oppgave`(
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
    fun `Adressebeskyttelse tilganger ved send til kontroll av oppgaver`(
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
                oppgave.sendTilKontroll(

                )
            }
        } else {
            shouldThrow<ManglendeTilgang> {
                oppgave.sendTilKontroll(

                )
            }
        }
    }
}
