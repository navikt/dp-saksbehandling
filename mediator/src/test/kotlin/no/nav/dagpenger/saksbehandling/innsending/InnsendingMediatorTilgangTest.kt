package no.nav.dagpenger.saksbehandling.innsending

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.FORTROLIG
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.STRENGT_FORTROLIG
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.IkkeTilgangTilEgneAnsatte
import no.nav.dagpenger.saksbehandling.ManglendeTilgangTilAdressebeskyttelse
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.TestHelper
import no.nav.dagpenger.saksbehandling.TilgangType
import no.nav.dagpenger.saksbehandling.TilgangType.FORTROLIG_ADRESSE
import no.nav.dagpenger.saksbehandling.TilgangType.SAKSBEHANDLER
import no.nav.dagpenger.saksbehandling.TilgangType.STRENGT_FORTROLIG_ADRESSE
import no.nav.dagpenger.saksbehandling.TilgangType.STRENGT_FORTROLIG_ADRESSE_UTLAND
import no.nav.dagpenger.saksbehandling.db.innsending.InnsendingRepository
import no.nav.dagpenger.saksbehandling.hendelser.FerdigstillInnsendingHendelse
import no.nav.dagpenger.saksbehandling.innsending.Innsending.Tilstand.UnderBehandling
import no.nav.dagpenger.saksbehandling.tilgangsstyring.SaksbehandlerErIkkeEier
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class InnsendingMediatorTilgangTest {
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
    fun `Vanlig saksbehandler har ikke tilgang til adressebeskyttet person`(
        adressebeskyttelseGradering: AdressebeskyttelseGradering,
        saksbehandlerTilgang: TilgangType,
        forventetTilgang: Boolean,
    ) {
        val innsending =
            TestHelper.lagInnsending(
                person = TestHelper.lagPerson(addresseBeskyttelseGradering = adressebeskyttelseGradering),
            )
        val saksbehandler =
            Saksbehandler(
                navIdent = "saksbehandler",
                grupper = emptySet(),
                tilganger = setOf(saksbehandlerTilgang),
            )
        val mediator =
            InnsendingMediator(
                innsendingRepository =
                    mockk<InnsendingRepository>().also {
                        every { it.hent(innsending.innsendingId) } returns innsending
                    },
                personMediator = mockk(),
                sakMediator = mockk(),
                oppgaveMediator = mockk(),
                innsendingBehandler = mockk(),
            )
        when (forventetTilgang) {
            true -> {
                shouldNotThrowAny {
                    mediator.hentInnsending(innsending.innsendingId, saksbehandler)
                }
            }
            false -> {
                shouldThrow<ManglendeTilgangTilAdressebeskyttelse> {
                    mediator.hentInnsending(innsending.innsendingId, saksbehandler)
                }
            }
        }
    }

    @Test
    fun `Vanlig saksbehandler har ikke tilgang til egne ansatte`() {
        val innsending =
            TestHelper.lagInnsending(
                person = TestHelper.lagPerson(skjermesSomEgneAnsatte = true),
            )
        val saksbehandler =
            Saksbehandler(
                navIdent = "saksbehandler",
                grupper = emptySet(),
                tilganger = setOf(SAKSBEHANDLER),
            )
        val saksbehandlerMedEgneAnsatteTilgang =
            Saksbehandler(
                navIdent = "saksbehandlerMedEgneAnsatteTilgang",
                grupper = emptySet(),
                tilganger = setOf(SAKSBEHANDLER, TilgangType.EGNE_ANSATTE),
            )
        val mediator =
            InnsendingMediator(
                innsendingRepository =
                    mockk<InnsendingRepository>().also {
                        every { it.hent(innsending.innsendingId) } returns innsending
                    },
                personMediator = mockk(),
                sakMediator = mockk(),
                oppgaveMediator = mockk(),
                innsendingBehandler = mockk(),
            )

        shouldThrow<IkkeTilgangTilEgneAnsatte> {
            mediator.hentInnsending(innsending.innsendingId, saksbehandler)
        }
        shouldNotThrowAny {
            mediator.hentInnsending(innsending.innsendingId, saksbehandlerMedEgneAnsatteTilgang)
        }
    }

    @Test
    fun `Bare saksbehandler som eier innsendingen kan ferdigstille den`() {
        val innsending =
            TestHelper.lagInnsending(
                behandlerIdent = "saksbehandlerSomEier",
                tilstand = UnderBehandling,
            )
        val saksbehandlerSomEier =
            Saksbehandler(
                navIdent = "saksbehandlerSomEier",
                grupper = emptySet(),
                tilganger = setOf(SAKSBEHANDLER),
            )
        val annenSaksbehandler =
            Saksbehandler(
                navIdent = "annenSaksbehandler",
                grupper = emptySet(),
                tilganger = setOf(SAKSBEHANDLER),
            )
        val mediator =
            InnsendingMediator(
                innsendingRepository =
                    mockk<InnsendingRepository>(relaxed = true).also {
                        every { it.hent(innsending.innsendingId) } returns innsending
                    },
                personMediator = mockk(),
                sakMediator = mockk(),
                oppgaveMediator = mockk(),
                innsendingBehandler = mockk(relaxed = true),
            )

        shouldThrow<SaksbehandlerErIkkeEier> {
            mediator.ferdigstill(
                FerdigstillInnsendingHendelse(
                    innsendingId = innsending.innsendingId,
                    utførtAv = annenSaksbehandler,
                    aksjon = Aksjon.Avslutt,
                ),
            )
        }
        shouldNotThrowAny {
            mediator.ferdigstill(
                FerdigstillInnsendingHendelse(
                    innsendingId = innsending.innsendingId,
                    utførtAv = saksbehandlerSomEier,
                    aksjon = Aksjon.Avslutt,
                ),
            )
        }
    }
}
