package no.nav.dagpenger.saksbehandling.db

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.PersonRepository
import java.util.UUID
import javax.sql.DataSource

class PostgresRepository(private val dataSource: DataSource) : PersonRepository {
    override fun lagre(person: Person) {
        using(sessionOf(dataSource)) { session ->
            session.transaction { transactionalSession ->
                transactionalSession.run(
                    queryOf(
                        //language=PostgreSQL
                        statement = """
                    INSERT INTO person_v1 (ident)
                    VALUES (:ident)
                    ON CONFLICT (ident) DO NOTHING
                        """.trimIndent(),
                        paramMap = mapOf(
                            "ident" to person.ident,
                        ),
                    ).asUpdate,
                )
            }
        }
    }

    override fun hent(ident: String): Person? {
        return using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement = """
                    SELECT ident
                    FROM  person_v1
                    WHERE ident = :ident
                    """.trimIndent(),
                    paramMap = mapOf(
                        "ident" to ident,
                    ),
                ).map { row -> Person(row.string("ident")) }.asSingle,
            )
        }
    }

    override fun hent(oppgaveId: UUID): Oppgave? {
        TODO("Not yet implemented")
    }

    override fun hentAlleOppgaver(): List<Oppgave> {
        TODO("Not yet implemented")
    }

    override fun hentAlleOppgaverMedTilstand(tilstand: Oppgave.Tilstand.Type): List<Oppgave> {
        TODO("Not yet implemented")
    }
}
