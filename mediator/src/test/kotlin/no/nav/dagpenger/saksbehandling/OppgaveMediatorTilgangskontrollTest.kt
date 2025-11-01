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
import no.nav.dagpenger.saksbehandling.TilgangType.EGNE_ANSATTE
import no.nav.dagpenger.saksbehandling.TilgangType.FORTROLIG_ADRESSE
import no.nav.dagpenger.saksbehandling.TilgangType.SAKSBEHANDLER
import no.nav.dagpenger.saksbehandling.TilgangType.STRENGT_FORTROLIG_ADRESSE
import no.nav.dagpenger.saksbehandling.TilgangType.STRENGT_FORTROLIG_ADRESSE_UTLAND
import no.nav.dagpenger.saksbehandling.db.oppgave.OppgaveRepository
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
        val oppgave =
            TestHelper.lagOppgave(person = TestHelper.lagPerson(addresseBeskyttelseGradering = adressebeskyttelseGradering))
        val saksbehandler =
            Saksbehandler(
                navIdent = "saksbehandler",
                grupper = setOf(),
                tilganger = setOf(saksbehandlerTilgang),
            )

        val oppgaveRepositoryMock =
            mockk<OppgaveRepository>().also { every { it.hentOppgave(oppgave.oppgaveId) } returns oppgave }
        val oppgaveMediator =
            OppgaveMediator(
                oppgaveRepository = oppgaveRepositoryMock,
                behandlingKlient = mockk(),
                utsendingMediator = mockk(),
                sakMediator = mockk(),
            )

        if (forventetTilgang) {
            shouldNotThrow<ManglendeTilgang> { oppgaveMediator.hentOppgave(oppgave.oppgaveId, saksbehandler) }
        } else {
            shouldThrow<ManglendeTilgang> { oppgaveMediator.hentOppgave(oppgave.oppgaveId, saksbehandler) }
        }
    }

    @Test
    fun `Saksbehandler må ha tilgang for å hente en oppgave med egne ansatte`() {
        val oppgave = TestHelper.lagOppgave(person = TestHelper.lagPerson(skjermesSomEgneAnsatte = true))

        val oppgaveRepositoryMock =
            mockk<OppgaveRepository>().also { every { it.hentOppgave(oppgave.oppgaveId) } returns oppgave }
        val oppgaveMediator =
            OppgaveMediator(
                oppgaveRepository = oppgaveRepositoryMock,
                behandlingKlient = mockk(),
                utsendingMediator = mockk(),
                sakMediator = mockk(),
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
