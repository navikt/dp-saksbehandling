package no.nav.dagpenger.saksbehandling.henvendelse

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.FORTROLIG
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.STRENGT_FORTROLIG
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.ManglendeTilgangTilAdressebeskyttelse
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.TestHelper
import no.nav.dagpenger.saksbehandling.TilgangType
import no.nav.dagpenger.saksbehandling.TilgangType.FORTROLIG_ADRESSE
import no.nav.dagpenger.saksbehandling.TilgangType.SAKSBEHANDLER
import no.nav.dagpenger.saksbehandling.TilgangType.STRENGT_FORTROLIG_ADRESSE
import no.nav.dagpenger.saksbehandling.TilgangType.STRENGT_FORTROLIG_ADRESSE_UTLAND
import no.nav.dagpenger.saksbehandling.db.henvendelse.HenvendelseRepository
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class HenvendelseMediatorTilgangTest {
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
        val henvendelse =
            TestHelper.lagHenvendelse(
                person = TestHelper.lagPerson(addresseBeskyttelseGradering = adressebeskyttelseGradering),
            )
        val saksbehandler =
            Saksbehandler(
                navIdent = "saksbehandler",
                grupper = emptySet(),
                tilganger = setOf(saksbehandlerTilgang),
            )
        val mediator =
            HenvendelseMediator(
                henvendelseRepository =
                    mockk<HenvendelseRepository>().also {
                        every { it.hent(henvendelse.henvendelseId) } returns henvendelse
                    },
                personMediator = mockk(),
                sakMediator = mockk(),
                oppgaveMediator = mockk(),
                henvendelseBehandler = mockk(),
            )
        when (forventetTilgang) {
            true -> {
                shouldNotThrowAny {
                    mediator.hentHenvendelse(henvendelse.henvendelseId, saksbehandler)
                }
            }
            false -> {
                shouldThrow<ManglendeTilgangTilAdressebeskyttelse> {
                    mediator.hentHenvendelse(henvendelse.henvendelseId, saksbehandler)
                }
            }
        }
    }
}
