package no.nav.dagpenger.saksbehandling.db

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.FERDIG_BEHANDLET
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class PostgresRepositoryTest {
    private val testPerson = Person(ident = "12345678901")
    private val behandlingId1 = UUIDv7.ny()
    private val oppgaveId = UUIDv7.ny()
    private val opprettetTidspunkt = ZonedDateTime.now().withZoneSameInstant(ZoneId.of("Europe/Oslo")).truncatedTo(ChronoUnit.SECONDS)
    private val oppgaveKlarTilBehandling = Oppgave(
        oppgaveId = oppgaveId,
        ident = testPerson.ident,
        emneknagger = setOf("Søknadsbehandling"),
        opprettet = opprettetTidspunkt,
        behandlingId = behandlingId1,
        tilstand = KLAR_TIL_BEHANDLING,
    )
    private val testBehandling = Behandling(
        behandlingId = behandlingId1,
        person = testPerson,
        opprettet = opprettetTidspunkt,
        oppgaver = mutableListOf(oppgaveKlarTilBehandling),
    )

    private val testPerson2 = Person(ident = "12345678902")
    private val behandlingId2 = UUIDv7.ny()
    private val oppgaveId2 = UUIDv7.ny()
    private val oppgaveId3 = UUIDv7.ny()
    private val oppgave2 = Oppgave(
        oppgaveId = oppgaveId2,
        ident = testPerson2.ident,
        emneknagger = setOf("Søknadsbehandling, Utland"),
        opprettet = opprettetTidspunkt,
        behandlingId = behandlingId2,
        tilstand = KLAR_TIL_BEHANDLING,
    )

    private val testBehandling2 = Behandling(
        behandlingId = behandlingId2,
        person = testPerson2,
        opprettet = opprettetTidspunkt,
        oppgaver = mutableListOf(oppgave2, oppgave2.copy(oppgaveId = oppgaveId3, tilstand = FERDIG_BEHANDLET)),
    )

    @Test
    fun `Skal kunne lagre og hente person`() {
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)
            repo.lagre(testPerson)

            val personFraDatabase = repo.finnPerson(testPerson.ident)
            personFraDatabase shouldBe testPerson
        }
    }

    @Test
    fun `Skal kunne slette behandling`() {
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)
            repo.lagre(testBehandling)
            repo.hentBehandling(testBehandling.behandlingId) shouldNotBe null
            repo.slettBehandling(testBehandling.behandlingId)

            assertThrows<DataNotFoundException> {
                repo.hentBehandling(testBehandling.behandlingId)
                repo.hentPerson(testPerson.ident)
                repo.hentOppgave(oppgaveId)
            }
        }
    }

    @Test
    fun `Exception hvis vi ikke får hentet person basert på ident`() {
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)

            shouldThrow<DataNotFoundException> {
                repo.hentPerson(testPerson.ident)
            }
        }
    }

    @Test
    fun `Exception hvis vi ikke får hentet behandling basert på behandlingId`() {
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)

            shouldThrow<DataNotFoundException> {
                repo.hentBehandling(behandlingId1)
            }
        }
    }

    @Test
    fun `Skal kunne lagre en behandling med oppgave`() {
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)
            repo.lagre(testBehandling)
            val behandlingFraDatabase = repo.hentBehandling(behandlingId1)
            behandlingFraDatabase shouldBe testBehandling
        }
    }

    @Test
    fun `Skal kunne lagre en behandling med oppgave flere ganger`() {
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)

            shouldNotThrowAny {
                repo.lagre(testBehandling)
                repo.lagre(testBehandling)
            }
        }
    }

    @Test
    fun `Skal kunne lagre og hente en oppgave`() {
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)
            repo.lagre(testBehandling)
            val oppgaveFraDatabase = repo.hentOppgave(oppgaveId)
            oppgaveFraDatabase shouldBe oppgaveKlarTilBehandling
        }
    }

    @Test
    fun `Skal kunne endre tilstand på en oppgave`() {
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)
            repo.lagre(testBehandling)
            repo.hentOppgave(oppgaveId).tilstand shouldBe KLAR_TIL_BEHANDLING

            repo.lagre(testBehandling.copy(oppgaver = mutableListOf(oppgaveKlarTilBehandling.copy(tilstand = FERDIG_BEHANDLET))))
            repo.hentOppgave(oppgaveId).tilstand shouldBe FERDIG_BEHANDLET
        }
    }

    @Test
    fun `Skal kunne lagre en behandling og hente den igjen på bakgrunn av en oppgaveId`() {
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)
            repo.lagre(testBehandling)
            val behandlingFraDatabase = repo.hentBehandlingFra(oppgaveId)
            behandlingFraDatabase shouldBe testBehandling
        }
    }

    @Test
    fun `Skal kunne hente oppgaver basert på tilstand`() {
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)
            repo.lagre(testBehandling)
            repo.lagre(testBehandling2)

            val ferdigBehandledeOppgaver = repo.hentAlleOppgaverMedTilstand(FERDIG_BEHANDLET)
            ferdigBehandledeOppgaver.size shouldBe 1
            ferdigBehandledeOppgaver.single().oppgaveId shouldBe oppgaveId3

            val oppgaverTilBehandling = repo.hentAlleOppgaverMedTilstand(KLAR_TIL_BEHANDLING)
            oppgaverTilBehandling.size shouldBe 2
            oppgaverTilBehandling.map { it.oppgaveId } shouldBe listOf(oppgaveId, oppgaveId2)
        }
    }

    @Test
    fun `Skal kunne hente alle oppgaver for en gitt person`() {
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)
            repo.lagre(testBehandling)
            repo.lagre(testBehandling2)

            val oppgaverTilPerson1 = repo.finnOppgaverFor(testPerson.ident)
            oppgaverTilPerson1.size shouldBe 1
            oppgaverTilPerson1.single().oppgaveId shouldBe oppgaveId

            val oppgaverTilPerson2 = repo.finnOppgaverFor(testPerson2.ident)
            oppgaverTilPerson2.size shouldBe 2
            oppgaverTilPerson2.map { it.oppgaveId } shouldBe listOf(oppgaveId2, oppgaveId3)
        }
    }
}
