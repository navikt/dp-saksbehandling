package no.nav.dagpenger.saksbehandling.db.person

import PersonMediator
import SkjermetPersonException
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.ints.exactly
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.api.Oppslag
import org.junit.jupiter.api.Test
import java.util.UUID

class PersonMediatorTest {
    private val testIdent = "12345678901"

    private val personRepositoryMock =
        mockk<PersonRepository>(relaxed = true).also {
            every {
                it.finnPerson(any<UUID>())
            } returns null
        }

    private val oppslagMock = mockk<Oppslag>()

    @Test
    fun `Skal ikke opprette person hvis personen er skjermet`() {
        val erSkjermetIdent = "12345678901"
        val ikkeSkjermet = "12345678902"
        coEvery { oppslagMock.erAdressebeskyttetPerson(testIdent) } returns UGRADERT
        coEvery { oppslagMock.erSkjermetPerson(erSkjermetIdent) } returns true
        coEvery { oppslagMock.erSkjermetPerson(ikkeSkjermet) } returns false

        val personMediator = PersonMediator(personRepositoryMock, oppslagMock)
        shouldThrow<SkjermetPersonException> {
            personMediator.finnEllerOpprettPerson(erSkjermetIdent)
        }

        shouldNotThrowAny { personMediator.finnEllerOpprettPerson(ikkeSkjermet) }

        verify(exactly = 1) {
            personRepositoryMock.lagre(
                Person(
                    id = any(),
                    ident = ikkeSkjermet,
                    skjermesSomEgneAnsatte = false,
                    adressebeskyttelseGradering = UGRADERT,
                ),
            )
        }
    }
}
