package no.nav.dagpenger.saksbehandling.db

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Oppgave.Tilstand.Type.KLAR_TIL_BEHANDLING
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class PostgresRepositoryTest {
    private val testPerson = Person(ident = "12345678901")
    private val behandlingId1 = UUIDv7.ny()
    private val oppgaveId = UUIDv7.ny()
    private val opprettetTidspunkt = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS)
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

    @Test
    fun `Skal kunne lagre og hente person`() {
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)
            repo.lagre(testPerson)

            val personFraDatabase = repo.hentPerson(testPerson.ident)
            personFraDatabase shouldBe testPerson
        }
    }

    @Test
    fun `Exception hvis vi ikke finner person basert på ident `() {
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)

            shouldThrow<DataNotFoundException> {
                repo.hentPerson(testPerson.ident)
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
    fun `Skal kunne lagre og hente en oppgave`() {
        withMigratedDb { ds ->
            val repo = PostgresRepository(ds)
            repo.lagre(testBehandling)
            val oppgaveFraDatabase = repo.hentOppgave(oppgaveId)
            oppgaveFraDatabase shouldBe oppgaveKlarTilBehandling
        }
    }

    @Test
    fun testLagre1() {
    }

    @Test
    fun hentBehandlingFra() {
    }

    @Test
    fun hentBehandling() {
    }

    @Test
    fun hentAlleOppgaver() {
    }

    @Test
    fun hentAlleOppgaverMedTilstand() {
    }

    @Test
    fun hentOppgave() {
    }

    @Test
    fun finnOppgaverFor() {
    }

    @Test
    fun hentPerson() {
    }
}
