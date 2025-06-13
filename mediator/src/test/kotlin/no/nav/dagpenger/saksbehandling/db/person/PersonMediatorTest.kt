package no.nav.dagpenger.saksbehandling.db.person

import AdresseBeeskyttetPersonException
import PersonMediator
import SkjermetPersonException
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.FORTROLIG
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.STRENGT_FORTROLIG
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.api.Oppslag
import org.junit.jupiter.api.Test

class PersonMediatorTest {
    private val testIdent = "12345678901"

    private val personRepositoryMock =
        mockk<PersonRepository>(relaxed = true).also {
            every {
                it.finnPerson(any<String>())
            } returns null
        }

    private val oppslagMock = mockk<Oppslag>()

    @Test
    fun `Skal ikke opprette person hvis personen er skjermet`() {
        val skjermetIdent = "12345678901"
        val ikkeSkjermetIdent = "12345678902"
        coEvery { oppslagMock.erAdressebeskyttetPerson(any()) } returns UGRADERT
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
        val fortroligBeskyttetIdent = "12345678901"
        val strengtFortroligBeskyttetIdent = "12345678903"
        val ikkeAddressebeskyttetIdent = "12345678902"
        coEvery { oppslagMock.erAdressebeskyttetPerson(strengtFortroligBeskyttetIdent) } returns STRENGT_FORTROLIG
        coEvery { oppslagMock.erAdressebeskyttetPerson(fortroligBeskyttetIdent) } returns FORTROLIG
        coEvery { oppslagMock.erAdressebeskyttetPerson(ikkeAddressebeskyttetIdent) } returns UGRADERT
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
}
