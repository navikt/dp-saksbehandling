package no.nav.dagpenger.saksbehandling.db

import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.AdresseBeskyttelseGradering.UGRADERT
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.db.Postgres.withMigratedDb
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import org.junit.jupiter.api.Test
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

class PostgresTriggerTest {
    @Test
    fun `Når en oppgave endres så skal endret_tidspunkt oppdateres`() {
        val testPerson =
            Person(
                ident = "12345678901",
                skjermesSomEgneAnsatte = false,
                adresseBeskyttelseGradering = UGRADERT,
            )
        val opprettet = LocalDateTime.now()
        val testBehandling = Behandling(behandlingId = UUIDv7.ny(), testPerson, opprettet = opprettet)
        val testOppgave =
            Oppgave(
                UUIDv7.ny(),
                ident = testPerson.ident,
                behandlingId = testBehandling.behandlingId,
                emneknagger = setOf("Hugga", "Bugga"),
                opprettet = opprettet,
                tilstand = Oppgave.KlarTilBehandling,
                behandling = testBehandling,
            )
        withMigratedDb { ds ->
            val repo = PostgresOppgaveRepository(ds)
            repo.lagre(testOppgave)
            val endretTidspunkt = ds.hentEndretTidspunkt(testOppgave.oppgaveId)
            Thread.sleep(100)

            val endretOppgave = testOppgave.copy(tilstand = Oppgave.UnderBehandling)
            repo.lagre(endretOppgave)
            val nyttEndretTidspunkt = ds.hentEndretTidspunkt(testOppgave.oppgaveId)
            nyttEndretTidspunkt.after(endretTidspunkt) shouldBe true
        }
    }
}

private fun DataSource.hentEndretTidspunkt(oppgaveId: UUID): Timestamp {
    return sessionOf(this).use { session ->
        session.run(
            queryOf(
                //language=PostgreSQL
                statement =
                    """
                    SELECT endret_tidspunkt
                    FROM   oppgave_v1
                    WHERE  id = :id
                    """.trimIndent(),
                paramMap = mapOf("id" to oppgaveId),
            ).map { row ->
                row.sqlTimestamp("endret_tidspunkt")
            }.asSingle,
        )
    }!!
}
