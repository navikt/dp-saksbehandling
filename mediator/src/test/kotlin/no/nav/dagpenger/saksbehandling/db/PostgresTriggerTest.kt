package no.nav.dagpenger.saksbehandling.db

import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.Person
import org.junit.jupiter.api.Test
import java.sql.Timestamp
import java.util.UUID
import javax.sql.DataSource

class PostgresTriggerTest {

    @Test
    fun `Når en person endres så skal sist_endret_tidspunkt oppdateres`() {
        val testPerson = Person(ident = "12345678901")
        Postgres.withMigratedDb { ds ->
            val repo = PostgresRepository(ds)
            repo.lagre(testPerson)
            val endretTidspunkt = ds.hentEndretTidspunkt(testPerson.id)
            Thread.sleep(100)

            val endretPerson = testPerson.copy(ident = "12345655555")
            repo.lagre(endretPerson)
            val nyttEndretTidspunkt = ds.hentEndretTidspunkt(testPerson.id)
            nyttEndretTidspunkt.after(endretTidspunkt) shouldBe true
        }
    }
}

private fun DataSource.hentEndretTidspunkt(personId: UUID): Timestamp {
    return sessionOf(this).use { session ->
        session.run(
            queryOf(
                //language=PostgreSQL
                statement = """
                SELECT endret_tidspunkt
                FROM   person_v1
                WHERE  id = :id
                """.trimIndent(),
                paramMap = mapOf("id" to personId),
            ).map { row ->
                row.sqlTimestamp("endret_tidspunkt")
            }.asSingle,
        )
    }!!
}
