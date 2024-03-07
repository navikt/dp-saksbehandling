package no.nav.dagpenger.saksbehandling.db

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class PostgresRepositoryTest {
    val testPersonEnIdent = "12345678901"
    val oppgaveTilTestPersonEn = Oppgave(
        oppgaveId = UUIDv7.ny(),
        opprettet = ZonedDateTime.now(),
        ident = testPersonEnIdent,
        emneknagger = setOf("knagg"),
        behandlingId = UUIDv7.ny(),
        tilstand = Oppgave.Tilstand.Type.OPPRETTET,
    )
    private val behandlingEnTilTestPersonEn = Behandling(
        behandlingId = UUIDv7.ny(),
        oppgave = oppgaveTilTestPersonEn,
    )
    val oppgaveToTilTestPersonEn = Oppgave(
        oppgaveId = UUIDv7.ny(),
        opprettet = ZonedDateTime.now(),
        ident = testPersonEnIdent,
        emneknagger = setOf("knagg"),
        behandlingId = UUIDv7.ny(),
        tilstand = Oppgave.Tilstand.Type.OPPRETTET,
    )
    private val behandlingToTilTestPersonEn = Behandling(
        behandlingId = UUIDv7.ny(),
        oppgave = oppgaveToTilTestPersonEn,
    )
    private val testPersonEn = Person(testPersonEnIdent).apply {
        this.behandlinger[behandlingEnTilTestPersonEn.behandlingId] = behandlingEnTilTestPersonEn
        this.behandlinger[behandlingToTilTestPersonEn.behandlingId] = behandlingToTilTestPersonEn
    }

    @Test
    fun `lagre og hente person med behandling`() {
        withMigratedDb { ds ->
            val postgresRepository = PostgresRepository(ds)

            shouldNotThrowAny {
                postgresRepository.lagre(testPersonEn)
            }

            postgresRepository.hent(testPersonEn.ident).let { personFraDatabase ->
                require(personFraDatabase != null)
                personFraDatabase.ident shouldBe testPersonEn.ident
                personFraDatabase.behandlinger shouldBe testPersonEn.behandlinger
            } shouldBe testPersonEn
        }
    }

    @Test
    fun `Lagring av person er idempotent`() {
        withMigratedDb { ds ->
            val postgresRepository = PostgresRepository(ds)
            val testPerson = Person(testPersonEn.ident)

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

            postgresRepository.lagre(Person(testPersonEn.ident))

            shouldNotThrowAny {
                postgresRepository.lagre(oppgaveTilTestPersonEn)
                postgresRepository.lagre(oppgaveToTilTestPersonEn)
            }

            postgresRepository.hent(oppgaveId = oppgaveTilTestPersonEn.oppgaveId) shouldBe oppgaveTilTestPersonEn
            postgresRepository.hentAlleOppgaver() shouldBe listOf(oppgaveTilTestPersonEn, oppgaveToTilTestPersonEn)
        }
    }
}
