package no.nav.dagpenger.saksbehandling.db.person

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.FORTROLIG
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.STRENGT_FORTROLIG
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.IkkeTilgangTilEgneAnsatte
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.Saksbehandler
import no.nav.dagpenger.saksbehandling.TilgangType.EGNE_ANSATTE
import no.nav.dagpenger.saksbehandling.TilgangType.SAKSBEHANDLER
import no.nav.dagpenger.saksbehandling.api.Oppslag
import org.junit.jupiter.api.Test

class PersonMediatorTest {
    private val oppslagMock = mockk<Oppslag>()

    @Test
    fun `Skal ikke opprette person hvis personen er skjermet`() {
        val personRepositoryMock =
            mockk<PersonRepository>(relaxed = true).also {
                every {
                    it.finnPerson(any<String>())
                } returns null
            }
        val skjermetIdent = "12345678901"
        val ikkeSkjermetIdent = "12345678902"
        coEvery { oppslagMock.adressebeskyttelseGradering(any()) } returns UGRADERT
        coEvery { oppslagMock.erSkjermetPerson(skjermetIdent) } returns true
        coEvery { oppslagMock.erSkjermetPerson(ikkeSkjermetIdent) } returns false

        val personMediator = PersonMediator(personRepositoryMock, oppslagMock)
        shouldThrow<SkjermetPersonException> {
            personMediator.finnEllerOpprettPerson(skjermetIdent)
        }

        shouldNotThrowAny { personMediator.finnEllerOpprettPerson(ikkeSkjermetIdent) }

        verify(exactly = 1) {
            personRepositoryMock.lagre(match { person -> person.ident == ikkeSkjermetIdent })
        }
    }

    @Test
    fun `Skal ikke opprette person hvis personen er addressebeskyttet`() {
        val personRepositoryMock =
            mockk<PersonRepository>(relaxed = true).also {
                every {
                    it.finnPerson(any<String>())
                } returns null
            }
        val fortroligBeskyttetIdent = "12345678901"
        val strengtFortroligBeskyttetIdent = "12345678903"
        val ikkeAddressebeskyttetIdent = "12345678902"
        coEvery { oppslagMock.adressebeskyttelseGradering(strengtFortroligBeskyttetIdent) } returns STRENGT_FORTROLIG
        coEvery { oppslagMock.adressebeskyttelseGradering(fortroligBeskyttetIdent) } returns FORTROLIG
        coEvery { oppslagMock.adressebeskyttelseGradering(ikkeAddressebeskyttetIdent) } returns UGRADERT
        coEvery { oppslagMock.erSkjermetPerson(any()) } returns false

        val personMediator = PersonMediator(personRepositoryMock, oppslagMock)
        shouldThrow<AdresseBeeskyttetPersonException> {
            personMediator.finnEllerOpprettPerson(fortroligBeskyttetIdent)
        }

        shouldThrow<AdresseBeeskyttetPersonException> {
            personMediator.finnEllerOpprettPerson(strengtFortroligBeskyttetIdent)
        }

        shouldNotThrowAny { personMediator.finnEllerOpprettPerson(ikkeAddressebeskyttetIdent) }

        verify(exactly = 1) {
            personRepositoryMock.lagre(match { person -> person.ident == ikkeAddressebeskyttetIdent })
        }
    }

    @Test
    fun `Skal hente person hvis saksbehandler har tilgang`() {
        val skjermetPerson =
            Person(
                ident = "12345600000",
                skjermesSomEgneAnsatte = true,
                adressebeskyttelseGradering = UGRADERT,
                inhabileNavIdenter = emptyList(),
            )
        val personRepositoryMock =
            mockk<PersonRepository>(relaxed = true).also {
                every {
                    it.hentPerson(personId = skjermetPerson.id)
                } returns skjermetPerson
                every {
                    it.hentPerson(ident = skjermetPerson.ident)
                } returns skjermetPerson
            }
        val vanligSaksbehandler =
            Saksbehandler(
                navIdent = "saksbehandler",
                grupper = emptySet(),
                tilganger = setOf(SAKSBEHANDLER),
            )
        val saksbehandlerMedEgneAnsatteTilgang =
            Saksbehandler(
                navIdent = "sbMedEgneAnsatteTilgang",
                grupper = emptySet(),
                tilganger = setOf(EGNE_ANSATTE, SAKSBEHANDLER),
            )
        val personMediator = PersonMediator(personRepositoryMock, oppslagMock)

        shouldThrow<IkkeTilgangTilEgneAnsatte> {
            personMediator.hentPerson(personId = skjermetPerson.id, saksbehandler = vanligSaksbehandler)
        }
        shouldNotThrowAny {
            personMediator.hentPerson(personId = skjermetPerson.id, saksbehandler = saksbehandlerMedEgneAnsatteTilgang)
        }
        shouldThrow<IkkeTilgangTilEgneAnsatte> {
            personMediator.hentPerson(ident = skjermetPerson.ident, saksbehandler = vanligSaksbehandler)
        }
        shouldNotThrowAny {
            personMediator.hentPerson(ident = skjermetPerson.ident, saksbehandler = saksbehandlerMedEgneAnsatteTilgang)
        }
    }
}
