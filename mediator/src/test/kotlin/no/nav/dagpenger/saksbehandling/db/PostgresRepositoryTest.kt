package no.nav.dagpenger.saksbehandling.db

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class PostgresRepositoryTest {
    private val testPerson = Person("12345678901")

    @Test
    fun `lagre og hente person`() {
        withMigratedDb { ds ->
            val postgresRepository = PostgresRepository(ds)

            shouldNotThrowAny {
                postgresRepository.lagre(testPerson)
            }

            postgresRepository.hent(testPerson.ident) shouldBe testPerson
        }
    }

    @Test
    fun `Lagring av person er idempotent`() {
        withMigratedDb { ds ->
            val postgresRepository = PostgresRepository(ds)
            val testPerson = Person(testPerson.ident)

            shouldNotThrowAny {
                postgresRepository.lagre(testPerson)
                postgresRepository.lagre(testPerson)
            }

            postgresRepository.hent(testPerson.ident) shouldBe testPerson
        }
    }

    @Test
    fun `Lagring og henting av oppgaver`() {
        withMigratedDb { ds ->
            val postgresRepository = PostgresRepository(ds)

            postgresRepository.lagre(Person(testPerson.ident))

            val testOppgave1 = Oppgave(
                oppgaveId = UUIDv7.ny(),
                opprettet = ZonedDateTime.now(),
                ident = testPerson.ident,
                emneknagger = setOf("knagg"),
                behandlingId = UUIDv7.ny(),
                tilstand = Oppgave.Tilstand.Type.OPPRETTET,
            )
            val testOppgave2 = Oppgave(
                oppgaveId = UUIDv7.ny(),
                opprettet = ZonedDateTime.now(),
                ident = testPerson.ident,
                emneknagger = setOf("knagg"),
                behandlingId = UUIDv7.ny(),
                tilstand = Oppgave.Tilstand.Type.OPPRETTET,
            )

            shouldNotThrowAny {
                postgresRepository.lagre(testOppgave1)
                postgresRepository.lagre(testOppgave2)
            }

            postgresRepository.hent(oppgaveId = testOppgave1.oppgaveId) shouldBe testOppgave1
            postgresRepository.hentAlleOppgaver() shouldBe listOf(testOppgave1, testOppgave2)
        }
    }
}
