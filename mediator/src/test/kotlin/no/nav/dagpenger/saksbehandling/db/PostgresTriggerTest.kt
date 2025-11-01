package no.nav.dagpenger.saksbehandling.db

import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.TestHelper
import no.nav.dagpenger.saksbehandling.db.oppgave.PostgresOppgaveRepository
import org.junit.jupiter.api.Test
import java.sql.Timestamp
import java.util.UUID
import javax.sql.DataSource

class PostgresTriggerTest {
    @Test
    fun `Når en oppgave endres så skal endret_tidspunkt oppdateres`() {
        DBTestHelper.withOppgave(TestHelper.testOppgave) { ds ->
            val repo = PostgresOppgaveRepository(ds)
            repo.lagre(TestHelper.testOppgave)
            val endretTidspunkt = ds.hentEndretTidspunkt(TestHelper.testOppgave.oppgaveId)
            Thread.sleep(100)

            val endretOppgave = TestHelper.testOppgave.copy(tilstand = Oppgave.UnderBehandling)
            repo.lagre(endretOppgave)
            val nyttEndretTidspunkt = ds.hentEndretTidspunkt(TestHelper.testOppgave.oppgaveId)
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
