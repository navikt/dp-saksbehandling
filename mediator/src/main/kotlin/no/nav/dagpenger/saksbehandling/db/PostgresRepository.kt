package no.nav.dagpenger.saksbehandling.db

import kotliquery.Session
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

    override fun lagre(oppgave: Oppgave) {
        using(sessionOf(dataSource)) { session ->
            session.transaction { transactionalSession ->
                transactionalSession.run(
                    queryOf(
                        //language=PostgreSQL
                        statement = """
                    INSERT INTO oppgave_v1 (id,person_ident,opprettet,behandling_id,tilstand)
                    VALUES (:id, :person_ident, :opprettet, :behandling_id, :tilstand)
                    ON CONFLICT (id) DO UPDATE SET tilstand = :tilstand
                        """.trimIndent(),
                        paramMap = mapOf(
                            "id" to oppgave.oppgaveId,
                            "person_ident" to oppgave.ident,
                            "opprettet" to oppgave.opprettet,
                            "behandling_id" to oppgave.behandlingId,
                            "tilstand" to oppgave.tilstand.name,
                        ),
                    ).asUpdate,
                )
            }
        }
    }

    override fun hent(oppgaveId: UUID): Oppgave? {
        return using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement = """
                    SELECT id,person_ident,opprettet,behandling_id,tilstand
                    FROM  oppgave_v1 
                    WHERE id = :id
                    """.trimIndent(),
                    paramMap = mapOf(
                        "id" to oppgaveId,
                    ),
                ).map { row ->
                    Oppgave.rehydrer(
                        oppgaveId = row.uuid("id"),
                        ident = row.string("person_ident"),
                        opprettet = row.zonedDateTime("opprettet"),
                        behandlingId = row.uuid("behandling_id"),
                        emneknagger = session.hentEmneknagger(oppgaveId),
                        tilstand = Oppgave.Tilstand.Type.valueOf(
                            row.string("tilstand"),
                        ),
                    )
                }.asSingle,
            )
        }
    }

    private fun Session.hentEmneknagger(oppgaveId: UUID): Set<String> {
        return emptySet()
    }

    override fun hentAlleOppgaver(): List<Oppgave> {
        TODO("Not yet implemented")
    }

    override fun hentAlleOppgaverMedTilstand(tilstand: Oppgave.Tilstand.Type): List<Oppgave> {
        TODO("Not yet implemented")
    }
}
