package no.nav.dagpenger.saksbehandling.db

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import org.junit.jupiter.api.Test

class PostgresRepositoryTest {
    private val testIdent = "13083826694"

    @Test
    fun `lagre og hente person`() {
        withMigratedDb { ds ->
            val postgresRepository = PostgresRepository(ds)
            val testPerson = Person(testIdent)

            shouldNotThrowAny {
                postgresRepository.lagre(testPerson)
            }

            shouldNotThrowAny {
                postgresRepository.lagre(testPerson)
            }

            postgresRepository.hent(testIdent) shouldBe testPerson
        }
    }
}
