package no.nav.dagpenger.saksbehandling.db.person

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.STRENGT_FORTROLIG
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.db.oppgave.DataNotFoundException
import no.nav.dagpenger.saksbehandling.lagPerson
import no.nav.dagpenger.saksbehandling.testPerson
import org.junit.jupiter.api.Test

class PostgresPersonRepositoryTest {
    @Test
    fun `Skal kunne lagre og hente person`() {
        withMigratedDb { ds ->
            val repo = PostgresPersonRepository(ds)
            repo.lagre(testPerson)

            val personFraDatabase = repo.finnPerson(testPerson.ident)
            personFraDatabase shouldBe testPerson
        }
    }

    @Test
    fun `Skal kunne oppdatere egen ansatt skjerming på en person`() {
        withMigratedDb { ds ->
            val repo = PostgresPersonRepository(ds)
            repo.lagre(testPerson)
            repo.finnPerson(testPerson.ident) shouldBe testPerson

            val oppdatertPerson = testPerson.copy(skjermesSomEgneAnsatte = true)
            repo.lagre(oppdatertPerson)
            repo.finnPerson(oppdatertPerson.ident) shouldBe oppdatertPerson
        }
    }

    @Test
    fun `Skal kunne oppdatere bare egen ansatt skjerming på en person`() {
        withMigratedDb { ds ->
            val repo = PostgresPersonRepository(ds)
            repo.lagre(testPerson)
            repo.hentPerson(testPerson.ident).skjermesSomEgneAnsatte shouldBe false

            repo.oppdaterSkjermingStatus(testPerson.ident, true)
            repo.hentPerson(testPerson.ident).skjermesSomEgneAnsatte shouldBe true
        }
    }

    @Test
    fun `Skal kunne oppdatere adresse beskyttet status på en person`() {
        withMigratedDb { ds ->
            val personRepository = PostgresPersonRepository(ds)
            personRepository.lagre(testPerson)
            personRepository.hentPerson(testPerson.ident).adressebeskyttelseGradering shouldBe UGRADERT

            personRepository.oppdaterAdressebeskyttetStatus(testPerson.ident, STRENGT_FORTROLIG)
            personRepository.hentPerson(testPerson.ident).adressebeskyttelseGradering shouldBe STRENGT_FORTROLIG
        }
    }

    @Test
    fun `Exception hvis vi ikke får hentet person basert på ident`() {
        withMigratedDb { ds ->
            val personRepository = PostgresPersonRepository(ds)

            shouldThrow<DataNotFoundException> {
                personRepository.hentPerson(testPerson.ident)
            }
        }
    }

    @Test
    fun `Sjekk om fødselsnumre eksisterer i vårt system`() {
        withMigratedDb { ds ->
            val personRepository = PostgresPersonRepository(ds)
            val (fnr1, fnr2, fnr3) = Triple("12345678910", "10987654321", "12345678931")

            personRepository.lagre(lagPerson(fnr1))
            personRepository.lagre(lagPerson(fnr2))
            personRepository.lagre(lagPerson("12345678941"))
            personRepository.eksistererIDPsystem(setOf(fnr1, fnr2, fnr3)) shouldBe setOf(fnr1, fnr2)
        }
    }
}
