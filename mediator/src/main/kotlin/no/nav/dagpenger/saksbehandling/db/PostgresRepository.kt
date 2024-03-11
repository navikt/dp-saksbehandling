package no.nav.dagpenger.saksbehandling.db

import kotliquery.Session
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
            session.lagre(person)
        }
    }

    private fun Session.lagre(person: Person) {
        //language=PostgreSQL
        run(
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
        sessionOf(dataSource).use { session ->
            session.transaction { tx ->
                tx.lagre(behandling.person)
                tx.lagre(behandling)
                tx.lagre(behandling.oppgaver)
            }
        }
    }

    override fun hentBehandling(behandlingId: UUID): Behandling {
        return Behandling.rehydrer(
            behandlingId = behandlingId,
            person = hentPersonFraBehandling(behandlingId),
            opprettet =
            oppgaver = hentOppgaverFraBehandling(behandlingId),,
        )
    }

    private fun Session.lagre(behandling: Behandling) {
        //language=PostgreSQL
        run(
            queryOf(
                statement = """
                     INSERT INTO behandling_v1 (id, person_id, opprettet) VALUES (:id, :person_id, :opprettet) 
                """.trimIndent(),
                paramMap = mapOf(
                    "id" to behandling.behandlingId,
                    "person_id" to behandling.person.id,
                    "opprettet" to behandling.opprettet,
                ),
            ).asUpdate,
        )
    }

    private fun Session.lagre(oppgaver: List<Oppgave>) {
        oppgaver.forEach { oppgave ->
            run(
                queryOf(
                    //language=PostgreSQL
                    statement = """
                     INSERT INTO oppgave_v1 (id, behandling_id, tilstand, opprettet) VALUES (:id, :person_id, :tilstand, :opprettet) 
                    """.trimIndent(),
                    paramMap = mapOf(
                        "id" to oppgave.oppgaveId,
                        "behandling_id" to oppgave.behandlingId,
                        "tilstand" to oppgave.tilstand.name,
                        "opprettet" to oppgave.opprettet,
                    ),
                ).asUpdate,
            )
        }
    }

    override fun lagre(oppgave: Oppgave) {
        TODO("Not yet implemented")
    }

    override fun hentBehandlingFra(oppgaveId: UUID): Behandling {
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
