package no.nav.dagpenger.saksbehandling

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.api.tilgangskontroll.AdressebeskyttelseTilgangskontroll
import no.nav.dagpenger.saksbehandling.api.tilgangskontroll.EgneAnsatteTilgangskontroll
import no.nav.dagpenger.saksbehandling.api.tilgangskontroll.IngenTilgangTilOppgaveException
import no.nav.dagpenger.saksbehandling.api.tilgangskontroll.Saksbehandler
import no.nav.dagpenger.saksbehandling.db.lagOppgave
import no.nav.dagpenger.saksbehandling.db.oppgave.TildelNesteOppgaveFilter
import no.nav.dagpenger.saksbehandling.hendelser.GodkjennBehandlingMedBrevIArena
import no.nav.dagpenger.saksbehandling.hendelser.GodkjentBehandlingHendelse
import no.nav.dagpenger.saksbehandling.hendelser.KlarTilKontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.SettOppgaveAnsvarHendelse
import no.nav.dagpenger.saksbehandling.hendelser.ToTrinnskontrollHendelse
import no.nav.dagpenger.saksbehandling.hendelser.UtsettOppgaveHendelse
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate
import java.util.UUID
import java.util.stream.Stream

class SecureOppgaveMediatorTest {
    companion object {
        private val saksbehandlerUtenEkstraTilganger =
            Saksbehandler("saksbehandlerUtenEkstraTilganger", setOf("SaksbehandlerADGruppe"))
        private val saksbehandlerMedEgneAnsatteTilgang =
            Saksbehandler(
                "saksbehandlerMedEgneAnsatteTilgang",
                setOf("EgneAnsatteADGruppe", "SaksbehandlerADGruppe"),
            )
        private val saksbehanderMedFortroligAdresseTilgang =
            Saksbehandler(
                "saksbehanderMedFortroligAdresseTilgang",
                setOf("FortroligADGruppe", "SaksbehandlerADGruppe"),
            )

        private val saksbehanderMedStrengtFortroligAdresseTilgang =
            Saksbehandler(
                "saksbehanderMedStrengtFortroligAdresseTilgang",
                setOf("StrengtFortroligADGruppe", "SaksbehandlerADGruppe"),
            )
        private val saksbehanderMedStrengtFortroligUtlandAdresseTilgang =
            Saksbehandler(
                "saksbehanderMedStrengtFortroligUtlandAdresseTilgang",
                setOf("StrengtFortroligUtlandADGruppe", "SaksbehandlerADGruppe"),
            )

        private val beslutterUtenEkstraTilganger =
            Saksbehandler(
                "beslutterUtenEkstraTilganger",
                setOf("BeslutterADGruppe", "SaksbehandlerADGruppe"),
            )

        private val superSaksbehandler =
            Saksbehandler(
                "superSaksbehandler",
                setOf(
                    "SaksbehandlerADGruppe",
                    "EgneAnsatteADGruppe",
                    "BeslutterADGruppe",
                    "FortroligADGruppe",
                    "StrengtFortroligADGruppe",
                    "StrengtFortroligUtlandADGruppe",
                ),
            )

        private val oppgaveUtenBeskyttelse = UUIDv7.ny()
        private val oppgaveUtenBeskyttelse2 = UUIDv7.ny()
        private val oppgaveMedFortroligAdresse = UUIDv7.ny()
        private val oppgaveMedStrengtFortroligAdresse = UUIDv7.ny()
        private val oppgaveMedStrengtFortroligUtlandAdresse = UUIDv7.ny()
        private val oppgaveMedEgneAnsatteSkjerming = UUIDv7.ny()

        private val adressebeskyttelseGraderingFunc = { oppgaveId: UUID ->
            when (oppgaveId) {
                oppgaveMedFortroligAdresse -> AdressebeskyttelseGradering.FORTROLIG
                oppgaveMedStrengtFortroligAdresse -> AdressebeskyttelseGradering.STRENGT_FORTROLIG
                oppgaveMedStrengtFortroligUtlandAdresse -> AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND
                oppgaveUtenBeskyttelse -> UGRADERT
                oppgaveUtenBeskyttelse2 -> UGRADERT
                oppgaveMedEgneAnsatteSkjerming -> UGRADERT
                else -> throw IllegalArgumentException("Ukjent oppgaveId: $oppgaveId")
            }
        }

        private val egneAnsatteSkjermingFunc = { oppgaveId: UUID ->
            when (oppgaveId) {
                oppgaveMedEgneAnsatteSkjerming -> true
                else -> false
            }
        }

        @JvmStatic
        private fun tester(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(saksbehandlerUtenEkstraTilganger, oppgaveUtenBeskyttelse, true),
                Arguments.of(saksbehandlerUtenEkstraTilganger, oppgaveMedFortroligAdresse, false),
                Arguments.of(saksbehandlerUtenEkstraTilganger, oppgaveMedStrengtFortroligAdresse, false),
                Arguments.of(saksbehandlerUtenEkstraTilganger, oppgaveMedStrengtFortroligUtlandAdresse, false),
                Arguments.of(saksbehandlerUtenEkstraTilganger, oppgaveMedEgneAnsatteSkjerming, false),
                Arguments.of(saksbehanderMedFortroligAdresseTilgang, oppgaveUtenBeskyttelse, true),
                Arguments.of(saksbehanderMedFortroligAdresseTilgang, oppgaveMedFortroligAdresse, true),
                Arguments.of(saksbehanderMedFortroligAdresseTilgang, oppgaveMedStrengtFortroligAdresse, false),
                Arguments.of(saksbehanderMedFortroligAdresseTilgang, oppgaveMedStrengtFortroligUtlandAdresse, false),
                Arguments.of(saksbehanderMedFortroligAdresseTilgang, oppgaveMedEgneAnsatteSkjerming, false),
                Arguments.of(saksbehanderMedStrengtFortroligAdresseTilgang, oppgaveUtenBeskyttelse, true),
                Arguments.of(saksbehanderMedStrengtFortroligAdresseTilgang, oppgaveMedFortroligAdresse, false),
                Arguments.of(saksbehanderMedStrengtFortroligAdresseTilgang, oppgaveMedStrengtFortroligAdresse, true),
                Arguments.of(
                    saksbehanderMedStrengtFortroligAdresseTilgang,
                    oppgaveMedStrengtFortroligUtlandAdresse,
                    false,
                ),
                Arguments.of(saksbehanderMedStrengtFortroligAdresseTilgang, oppgaveMedEgneAnsatteSkjerming, false),
                Arguments.of(saksbehanderMedStrengtFortroligUtlandAdresseTilgang, oppgaveUtenBeskyttelse, true),
                Arguments.of(saksbehanderMedStrengtFortroligUtlandAdresseTilgang, oppgaveMedFortroligAdresse, false),
                Arguments.of(
                    saksbehanderMedStrengtFortroligUtlandAdresseTilgang,
                    oppgaveMedStrengtFortroligAdresse,
                    false,
                ),
                Arguments.of(
                    saksbehanderMedStrengtFortroligUtlandAdresseTilgang,
                    oppgaveMedStrengtFortroligUtlandAdresse,
                    true,
                ),
                Arguments.of(
                    saksbehanderMedStrengtFortroligUtlandAdresseTilgang,
                    oppgaveMedEgneAnsatteSkjerming,
                    false,
                ),
                Arguments.of(saksbehandlerMedEgneAnsatteTilgang, oppgaveUtenBeskyttelse, true),
                Arguments.of(saksbehandlerMedEgneAnsatteTilgang, oppgaveMedFortroligAdresse, false),
                Arguments.of(saksbehandlerMedEgneAnsatteTilgang, oppgaveMedStrengtFortroligAdresse, false),
                Arguments.of(saksbehandlerMedEgneAnsatteTilgang, oppgaveMedStrengtFortroligUtlandAdresse, false),
                Arguments.of(saksbehandlerMedEgneAnsatteTilgang, oppgaveMedEgneAnsatteSkjerming, true),
                Arguments.of(superSaksbehandler, oppgaveUtenBeskyttelse, true),
                Arguments.of(superSaksbehandler, oppgaveMedFortroligAdresse, true),
                Arguments.of(superSaksbehandler, oppgaveMedStrengtFortroligAdresse, true),
                Arguments.of(superSaksbehandler, oppgaveMedStrengtFortroligUtlandAdresse, true),
                Arguments.of(superSaksbehandler, oppgaveMedEgneAnsatteSkjerming, true),
            )
        }

        @JvmStatic
        private fun beslutterTest(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(beslutterUtenEkstraTilganger, oppgaveUtenBeskyttelse, true),
                Arguments.of(beslutterUtenEkstraTilganger, oppgaveMedEgneAnsatteSkjerming, false),
                Arguments.of(beslutterUtenEkstraTilganger, oppgaveMedFortroligAdresse, false),
                Arguments.of(beslutterUtenEkstraTilganger, oppgaveMedStrengtFortroligAdresse, false),
                Arguments.of(beslutterUtenEkstraTilganger, oppgaveMedStrengtFortroligUtlandAdresse, false),
                Arguments.of(saksbehandlerUtenEkstraTilganger, oppgaveUtenBeskyttelse, false),
                Arguments.of(saksbehandlerMedEgneAnsatteTilgang, oppgaveMedEgneAnsatteSkjerming, false),
                Arguments.of(superSaksbehandler, oppgaveUtenBeskyttelse, true),
            )
        }

        private fun lageMediatorMock(block: OppgaveMediator.() -> Unit): SecureOppgaveMediator {
            return SecureOppgaveMediator(
                adressebeskyttelseTilgangskontroll =
                    AdressebeskyttelseTilgangskontroll(
                        strengtFortroligGruppe = "StrengtFortroligADGruppe",
                        strengtFortroligUtlandGruppe = "StrengtFortroligUtlandADGruppe",
                        fortroligGruppe = "FortroligADGruppe",
                        adressebeskyttelseGraderingFun = adressebeskyttelseGraderingFunc,
                    ),
                egneAnsatteTilgangskontroll =
                    EgneAnsatteTilgangskontroll(
                        tillatteGrupper = setOf("EgneAnsatteADGruppe"),
                        skjermesSomEgneAnsatteFun = egneAnsatteSkjermingFunc,
                    ),
                oppgaveMediator =
                    mockk<OppgaveMediator>(relaxed = true).also {
                        block(it)
                    },
            )
        }
    }

    @ParameterizedTest
    @MethodSource("tester")
    fun `Tilgangskontroll for hentOppgave`(
        saksbehandler: Saksbehandler,
        oppgaveId: UUID,
        forventetTilgang: Boolean,
    ) {
        val testOppgave = lagOppgave(oppgaveId = oppgaveId)
        val mediator =
            lageMediatorMock {
                every { hentOppgave(oppgaveId) } returns testOppgave
            }
        if (forventetTilgang) {
            mediator.hentOppgave(oppgaveId, saksbehandler) shouldBe testOppgave
        } else {
            shouldThrow<IngenTilgangTilOppgaveException> {
                mediator.hentOppgave(oppgaveId, saksbehandler)
            }
        }
    }

    @ParameterizedTest
    @MethodSource("tester")
    fun `Tilgangskontroll for tildel oppgave`(
        saksbehandler: Saksbehandler,
        oppgaveId: UUID,
        forventetTilgang: Boolean,
    ) {
        val testOppgave = lagOppgave(oppgaveId = oppgaveId)
        val hendelse =
            SettOppgaveAnsvarHendelse(
                oppgaveId = testOppgave.oppgaveId,
                ansvarligIdent = saksbehandler.navIdent,
                utførtAv = saksbehandler.navIdent,
            )
        val mediator =
            lageMediatorMock {
                every { tildelOppgave(hendelse) } returns testOppgave
            }

        if (forventetTilgang) {
            mediator.tildelOppgave(hendelse, saksbehandler) shouldBe testOppgave
        } else {
            shouldThrow<IngenTilgangTilOppgaveException> {
                mediator.tildelOppgave(hendelse, saksbehandler)
            }
        }
    }

    @ParameterizedTest
    @MethodSource("tester")
    fun `Tilgangskontroll for utsett oppgave`(
        saksbehandler: Saksbehandler,
        oppgaveId: UUID,
        forventetTilgang: Boolean,
    ) {
        val hendelse =
            UtsettOppgaveHendelse(
                oppgaveId = oppgaveId,
                navIdent = saksbehandler.navIdent,
                utsattTil = LocalDate.now().plusDays(1),
                beholdOppgave = false,
                utførtAv = saksbehandler.navIdent,
            )
        val mediator =
            lageMediatorMock {
                every { utsettOppgave(hendelse) } just runs
            }

        if (forventetTilgang) {
            shouldNotThrowAny {
                mediator.utsettOppgave(hendelse, saksbehandler)
            }
        } else {
            shouldThrow<IngenTilgangTilOppgaveException> {
                mediator.utsettOppgave(hendelse, saksbehandler)
            }
        }
    }

    @ParameterizedTest
    @MethodSource("tester")
    fun `Tilgangskontroll for gjør klar til kontroll`(
        saksbehandler: Saksbehandler,
        oppgaveId: UUID,
        forventetTilgang: Boolean,
    ) {
        val hendelse =
            KlarTilKontrollHendelse(
                oppgaveId = oppgaveId,
                utførtAv = saksbehandler.navIdent,
            )
        val mediator =
            lageMediatorMock {
                every { gjørKlarTilKontroll(hendelse) } just runs
            }

        if (forventetTilgang) {
            shouldNotThrowAny {
                mediator.gjørKlarTilKontroll(hendelse, saksbehandler)
            }
        } else {
            shouldThrow<IngenTilgangTilOppgaveException> {
                mediator.gjørKlarTilKontroll(hendelse, saksbehandler)
            }
        }
    }

    @ParameterizedTest
    @MethodSource("beslutterTest")
    fun `Tilgangskontroll for utfør totrinnskontroll`(
        saksbehandler: Saksbehandler,
        oppgaveId: UUID,
        forventetTilgang: Boolean,
    ) {
        val hendelse =
            ToTrinnskontrollHendelse(
                oppgaveId = oppgaveId,
                ansvarligIdent = saksbehandler.navIdent,
                utførtAv = saksbehandler.navIdent,
            )
        val mediator =
            lageMediatorMock {
                every { tildelTotrinnskontroll(hendelse) } just runs
            }

        if (forventetTilgang) {
            shouldNotThrowAny {
                mediator.tildelTotrinnskontroll(hendelse, saksbehandler)
            }
        } else {
            shouldThrow<IngenTilgangTilOppgaveException> {
                mediator.tildelTotrinnskontroll(hendelse, saksbehandler)
            }
        }
    }

    @Test
    fun `tilgangskontroll for ferdigstilling av oppgave som er under behandling`() {
        val oppgaveUnderBehandling =
            lagOppgave(
                oppgaveId = oppgaveUtenBeskyttelse,
                tilstand = Oppgave.UnderBehandling,
                saksbehandlerIdent = saksbehandlerUtenEkstraTilganger.navIdent,
            )
        val godkjentBehandlingHendelse =
            GodkjentBehandlingHendelse(
                oppgaveId = oppgaveUnderBehandling.oppgaveId,
                meldingOmVedtak = "html",
                utførtAv = saksbehandlerUtenEkstraTilganger.navIdent,
            )
        lageMediatorMock {
            every { hentOppgave(oppgaveUnderBehandling.oppgaveId) } returns oppgaveUnderBehandling
            every { ferdigstillOppgave(godkjentBehandlingHendelse, any()) } just Runs
        }.let {
            shouldNotThrowAny {
                it.ferdigstillOppgave(godkjentBehandlingHendelse, saksbehandlerUtenEkstraTilganger, "token")
            }

            shouldThrow<IngenTilgangTilOppgaveException> {
                it.ferdigstillOppgave(
                    godkjentBehandlingHendelse,
                    superSaksbehandler,
                    "token",
                )
            }
        }
    }

    @Test
    fun `tilgangskontroll for ferdigstilling av oppgave som er under kontroll`() {
        val oppgaveUnderKontroll =
            lagOppgave(
                oppgaveId = oppgaveUtenBeskyttelse,
                tilstand = Oppgave.UnderKontroll,
                saksbehandlerIdent = beslutterUtenEkstraTilganger.navIdent,
            )

        val oppgaveUnderKontrollEidAvSaksbehandler =
            lagOppgave(
                oppgaveId = oppgaveUtenBeskyttelse2,
                tilstand = Oppgave.UnderKontroll,
                saksbehandlerIdent = saksbehandlerUtenEkstraTilganger.navIdent,
            )

        val godkjentBehandlingHendelse =
            GodkjentBehandlingHendelse(
                oppgaveId = oppgaveUnderKontroll.oppgaveId,
                meldingOmVedtak = "html",
                utførtAv = beslutterUtenEkstraTilganger.navIdent,
            )
        lageMediatorMock {
            every { hentOppgave(oppgaveUnderKontroll.oppgaveId) } returns oppgaveUnderKontroll
            every { hentOppgave(oppgaveUnderKontrollEidAvSaksbehandler.oppgaveId) } returns oppgaveUnderKontrollEidAvSaksbehandler
            every { ferdigstillOppgave(godkjentBehandlingHendelse, any()) } just Runs
        }.let {
            shouldNotThrowAny {
                it.ferdigstillOppgave(godkjentBehandlingHendelse, beslutterUtenEkstraTilganger, "token")
            }

            shouldThrow<IngenTilgangTilOppgaveException> {
                it.ferdigstillOppgave(
                    godkjentBehandlingHendelse,
                    superSaksbehandler,
                    "token",
                )
            }

            shouldThrow<IngenTilgangTilOppgaveException> {
                it.ferdigstillOppgave(
                    GodkjentBehandlingHendelse(
                        oppgaveId = oppgaveUnderKontrollEidAvSaksbehandler.oppgaveId,
                        meldingOmVedtak = "html",
                        utførtAv = saksbehandlerUtenEkstraTilganger.navIdent,
                    ),
                    saksbehandlerUtenEkstraTilganger,
                    "token",
                )
            }
        }
    }

    @Test
    fun `tilgangskontroll for ferdigstilling av oppgave  uten brev som er under behandling`() {
        val oppgaveUnderBehandling =
            lagOppgave(
                oppgaveId = oppgaveUtenBeskyttelse,
                tilstand = Oppgave.UnderBehandling,
                saksbehandlerIdent = saksbehandlerUtenEkstraTilganger.navIdent,
            )
        val godkjentBehandlingHendelse =
            GodkjennBehandlingMedBrevIArena(
                oppgaveId = oppgaveUnderBehandling.oppgaveId,
                utførtAv = saksbehandlerUtenEkstraTilganger.navIdent,
            )
        lageMediatorMock {
            every { hentOppgave(oppgaveUnderBehandling.oppgaveId) } returns oppgaveUnderBehandling
            every { ferdigstillOppgave(godkjentBehandlingHendelse, any()) } just Runs
        }.let {
            shouldNotThrowAny {
                it.ferdigstillOppgave(godkjentBehandlingHendelse, saksbehandlerUtenEkstraTilganger, "token")
            }

            shouldThrow<IngenTilgangTilOppgaveException> {
                it.ferdigstillOppgave(
                    godkjentBehandlingHendelse,
                    superSaksbehandler,
                    "token",
                )
            }
        }
    }

    @Test
    fun `tilgangskontroll for ferdigstilling av oppgave uten brev som er under kontroll`() {
        val oppgaveUnderKontroll =
            lagOppgave(
                oppgaveId = oppgaveUtenBeskyttelse,
                tilstand = Oppgave.UnderKontroll,
                saksbehandlerIdent = beslutterUtenEkstraTilganger.navIdent,
            )

        val oppgaveUnderKontrollEidAvSaksbehandler =
            lagOppgave(
                oppgaveId = oppgaveUtenBeskyttelse2,
                tilstand = Oppgave.UnderKontroll,
                saksbehandlerIdent = saksbehandlerUtenEkstraTilganger.navIdent,
            )

        val godkjentBehandlingHendelse =
            GodkjennBehandlingMedBrevIArena(
                oppgaveId = oppgaveUnderKontroll.oppgaveId,
                utførtAv = beslutterUtenEkstraTilganger.navIdent,
            )
        lageMediatorMock {
            every { hentOppgave(oppgaveUnderKontroll.oppgaveId) } returns oppgaveUnderKontroll
            every { hentOppgave(oppgaveUnderKontrollEidAvSaksbehandler.oppgaveId) } returns oppgaveUnderKontrollEidAvSaksbehandler
            every { ferdigstillOppgave(godkjentBehandlingHendelse, any()) } just Runs
        }.let {
            shouldNotThrowAny {
                it.ferdigstillOppgave(godkjentBehandlingHendelse, beslutterUtenEkstraTilganger, "token")
            }

            shouldThrow<IngenTilgangTilOppgaveException> {
                it.ferdigstillOppgave(
                    godkjentBehandlingHendelse,
                    superSaksbehandler,
                    "token",
                )
            }

            shouldThrow<IngenTilgangTilOppgaveException> {
                it.ferdigstillOppgave(
                    GodkjentBehandlingHendelse(
                        oppgaveId = oppgaveUnderKontrollEidAvSaksbehandler.oppgaveId,
                        meldingOmVedtak = "html",
                        utførtAv = saksbehandlerUtenEkstraTilganger.navIdent,
                    ),
                    saksbehandlerUtenEkstraTilganger,
                    "token",
                )
            }
        }
    }

    @Test
    fun `tildel neste oppgave delegerer til noe noe`() {
        val saksbehandler = Saksbehandler("ident", emptySet())

        val slot = mutableListOf<TildelNesteOppgaveFilter>()
        val oppgaveMediator =
            mockk<OppgaveMediator>().also {
                every {
                    it.tildelNesteOppgaveTil(
                        saksbehandler.navIdent,
                        capture(slot),
                    )
                } returns null
            }
        SecureOppgaveMediator(
            oppgaveMediator,
        ).let {
            it.tildelNesteOppgaveTil(
                saksbehandler = saksbehandler,
                queryString = "",
            ) shouldBe null
        }

        slot.single().let {
            it.harTilgangTilAdressebeskyttelser shouldBe setOf(UGRADERT)
            it.harTilgangTilEgneAnsatte shouldBe false
        }
    }
}
