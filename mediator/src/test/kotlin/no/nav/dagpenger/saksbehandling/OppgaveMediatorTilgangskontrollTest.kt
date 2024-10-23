package no.nav.dagpenger.saksbehandling

import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.FORTROLIG
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.STRENGT_FORTROLIG
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.ManglendeTilgang
import no.nav.dagpenger.saksbehandling.Oppgave.UnderBehandling
import no.nav.dagpenger.saksbehandling.TilgangType.EGNE_ANSATTE
import no.nav.dagpenger.saksbehandling.TilgangType.FORTROLIG_ADRESSE
import no.nav.dagpenger.saksbehandling.TilgangType.SAKSBEHANDLER
import no.nav.dagpenger.saksbehandling.TilgangType.STRENGT_FORTROLIG_ADRESSE
import no.nav.dagpenger.saksbehandling.TilgangType.STRENGT_FORTROLIG_ADRESSE_UTLAND
import no.nav.dagpenger.saksbehandling.behandling.BehandlingKlient
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
import no.nav.dagpenger.saksbehandling.hendelser.GodkjennBehandlingMedBrevIArena
import no.nav.dagpenger.saksbehandling.hendelser.GodkjentBehandlingHendelse
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class OppgaveMediatorTilgangskontrollTest {
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
    fun `Saksbehandler må ha tilgang for å hente en oppgave med adressebeskyttelse`(
        adressebeskyttelseGradering: AdressebeskyttelseGradering,
        saksbehandlerTilgang: TilgangType,
        forventetTilgang: Boolean,
    ) {
        val oppgave = lagOppgave(adressebeskyttelseGradering = adressebeskyttelseGradering)
        val saksbehandler =
            Saksbehandler(
                navIdent = "saksbehandler",
                grupper = setOf(),
                tilganger = setOf(saksbehandlerTilgang),
            )

        val oppgaveRepositoryMock = mockk<OppgaveRepository>().also { every { it.hentOppgave(oppgave.oppgaveId) } returns oppgave }
        val oppgaveMediator =
            OppgaveMediator(
                repository = oppgaveRepositoryMock,
                skjermingKlient = mockk(),
                pdlKlient = mockk(),
                behandlingKlient = mockk(),
                utsendingMediator = mockk(),
            )

        if (forventetTilgang) {
            shouldNotThrow<ManglendeTilgang> { oppgaveMediator.hentOppgave(oppgave.oppgaveId, saksbehandler) }
        } else {
            shouldThrow<ManglendeTilgang> { oppgaveMediator.hentOppgave(oppgave.oppgaveId, saksbehandler) }
        }
    }

    @ParameterizedTest
    @MethodSource("adressebeskyttelseTester")
    fun `Saksbehandler må ha tilgang for å ferdigstille en oppgave med adressebeskyttelse`(
        adressebeskyttelseGradering: AdressebeskyttelseGradering,
        saksbehandlerTilgang: TilgangType,
        forventetTilgang: Boolean,
    ) {
        val navIdent = "saksbehandler"
        val saksbehandler =
            Saksbehandler(
                navIdent = navIdent,
                grupper = setOf(),
                tilganger = setOf(saksbehandlerTilgang),
            )
        val oppgave =
            lagOppgave(
                tilstand = UnderBehandling,
                adressebeskyttelseGradering = adressebeskyttelseGradering,
                saksbehandlerIdent = navIdent,
            )

        val oppgaveRepositoryMock =
            mockk<OppgaveRepository>(
                relaxed = true,
            ).also { every { it.hentOppgave(oppgave.oppgaveId) } returns oppgave }
        val oppgaveMediator =
            OppgaveMediator(
                repository = oppgaveRepositoryMock,
                skjermingKlient = mockk(relaxed = true),
                pdlKlient = mockk(relaxed = true),
                behandlingKlient =
                    mockk<BehandlingKlient>(relaxed = true).also {
                        every { it.godkjennBehandling(any(), any(), any()) } returns Result.success(Unit)
                    },
                utsendingMediator = mockk(relaxed = true),
            )

        val godkjentBehandlingHendelse =
            GodkjentBehandlingHendelse(
                oppgaveId = oppgave.oppgaveId,
                meldingOmVedtak = "<HTML av noe slag>",
                utførtAv = saksbehandler,
            )

        if (forventetTilgang) {
            shouldNotThrow<ManglendeTilgang> { oppgaveMediator.ferdigstillOppgave(godkjentBehandlingHendelse, "token") }
        } else {
            shouldThrow<ManglendeTilgang> { oppgaveMediator.ferdigstillOppgave(godkjentBehandlingHendelse, "token") }
        }
    }

    @ParameterizedTest
    @MethodSource("adressebeskyttelseTester")
    fun `Saksbehandler må ha tilgang for å ferdigstille en oppgave med brev i Arena med adressebeskyttelse`(
        adressebeskyttelseGradering: AdressebeskyttelseGradering,
        saksbehandlerTilgang: TilgangType,
        forventetTilgang: Boolean,
    ) {
        val navIdent = "saksbehandler"
        val saksbehandler =
            Saksbehandler(
                navIdent = navIdent,
                grupper = setOf(),
                tilganger = setOf(saksbehandlerTilgang),
            )
        val oppgave =
            lagOppgave(
                tilstand = UnderBehandling,
                adressebeskyttelseGradering = adressebeskyttelseGradering,
                saksbehandlerIdent = navIdent,
            )

        val oppgaveRepositoryMock =
            mockk<OppgaveRepository>(
                relaxed = true,
            ).also { every { it.hentOppgave(oppgave.oppgaveId) } returns oppgave }
        val oppgaveMediator =
            OppgaveMediator(
                repository = oppgaveRepositoryMock,
                skjermingKlient = mockk(relaxed = true),
                pdlKlient = mockk(relaxed = true),
                behandlingKlient =
                    mockk<BehandlingKlient>(relaxed = true).also {
                        every { it.godkjennBehandling(any(), any(), any()) } returns Result.success(Unit)
                    },
                utsendingMediator = mockk(relaxed = true),
            )

        val godkjentBehandlingHendelse =
            GodkjennBehandlingMedBrevIArena(
                oppgaveId = oppgave.oppgaveId,
                utførtAv = saksbehandler,
            )

        if (forventetTilgang) {
            shouldNotThrow<ManglendeTilgang> { oppgaveMediator.ferdigstillOppgave(godkjentBehandlingHendelse, "token") }
        } else {
            shouldThrow<ManglendeTilgang> { oppgaveMediator.ferdigstillOppgave(godkjentBehandlingHendelse, "token") }
        }
    }

    @Test
    fun `Saksbehandler må ha tilgang for å hente en oppgave med egne ansatte`() {
        val oppgave = lagOppgave(skjermesSomEgneAnsatte = true)

        val oppgaveRepositoryMock = mockk<OppgaveRepository>().also { every { it.hentOppgave(oppgave.oppgaveId) } returns oppgave }
        val oppgaveMediator =
            OppgaveMediator(
                repository = oppgaveRepositoryMock,
                skjermingKlient = mockk(),
                pdlKlient = mockk(),
                behandlingKlient = mockk(),
                utsendingMediator = mockk(),
            )

        shouldNotThrow<ManglendeTilgang> {
            oppgaveMediator.hentOppgave(
                oppgave.oppgaveId,
                Saksbehandler(
                    navIdent = "saksbehandler 1",
                    grupper = setOf(),
                    tilganger = setOf(EGNE_ANSATTE),
                ),
            )
        }

        shouldThrow<ManglendeTilgang> {
            oppgaveMediator.hentOppgave(
                oppgave.oppgaveId,
                Saksbehandler(
                    navIdent = "saksbehandler 2",
                    grupper = setOf(),
                    tilganger = emptySet(),
                ),
            )
        }
    }
}
