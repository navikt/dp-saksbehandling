package no.nav.dagpenger.saksbehandling.db

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Person
import java.util.UUID
import javax.sql.DataSource

class DataNotFoundException(message: String) : RuntimeException(message)
class PostgresRepository(private val dataSource: DataSource) : Repository {
    override fun lagre(person: Person) {
        sessionOf(dataSource).use { session ->
            //language=PostgreSQL
            session.run(
                queryOf(
                    statement = """
                     INSERT INTO person_v1 (id, ident) VALUES (:id, :ident) ON CONFLICT (id) DO UPDATE SET ident = :ident
                    """.trimIndent(),
                    paramMap = mapOf(
                        "id" to person.id,
                        "ident" to person.ident,
                    ),
                ).asUpdate,
            )
        }
    }

    override fun hentPerson(ident: String): Person {
        sessionOf(dataSource).use { session ->
            return session.run(
                queryOf(
                    statement = """
                    SELECT * FROM person_v1 WHERE ident = :ident
                    """.trimIndent(),
                    paramMap = mapOf(
                        "ident" to ident,
                    ),
                ).map { row ->
                    Person(
                        id = row.uuid("id"),
                        ident = row.string("ident"),
                    )
                }.asSingle,
            ) ?: throw DataNotFoundException("Fant ikke person med ident $ident")
        }
    }

    override fun lagre(behandling: Behandling) {
        TODO("Not yet implemented")
    }

    override fun lagre(oppgave: Oppgave) {
        TODO("Not yet implemented")
    }

    override fun hentBehandlingFra(oppgaveId: UUID): Behandling {
        TODO("Not yet implemented")
    }

    override fun hentBehandling(behandlingId: UUID): Behandling {
        TODO("Not yet implemented")
    }

    override fun hentAlleOppgaver(): List<Oppgave> {
        TODO("Not yet implemented")
    }

    override fun hentAlleOppgaverMedTilstand(tilstand: Oppgave.Tilstand.Type): List<Oppgave> {
        TODO("Not yet implemented")
    }

    override fun hentOppgave(oppgaveId: UUID): Oppgave? {
        TODO("Not yet implemented")
    }

    override fun finnOppgaverFor(ident: String): List<Oppgave> {
        TODO("Not yet implemented")
    }
}
