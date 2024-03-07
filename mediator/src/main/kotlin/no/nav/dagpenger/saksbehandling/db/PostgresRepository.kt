package no.nav.dagpenger.saksbehandling.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.dagpenger.saksbehandling.Behandling
import no.nav.dagpenger.saksbehandling.Oppgave
import no.nav.dagpenger.saksbehandling.Person
import java.util.UUID
import javax.sql.DataSource

class PostgresRepository(private val dataSource: DataSource) : PersonRepository, OppgaveRepository {
    override fun lagre(person: Person) {
        using(sessionOf(dataSource)) { session ->
            session.transaction { tx ->
                tx.run(
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
                person.behandlinger.forEach { (_, behandling) ->
                    tx.lagre(behandling.oppgave)
                }
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
            )?.also { person ->
                hentOppgaverFor(person.ident).forEach { oppgave ->
                    person.behandlinger[oppgave.behandlingId] = Behandling(oppgave.behandlingId, oppgave)
                }
            }
        }
    }

    private fun hentOppgaverFor(ident: String): List<Oppgave> {
        return using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement = """
                    SELECT id,person_ident,opprettet,behandling_id,tilstand
                    FROM  oppgave_v1 
                    WHERE person_ident = :person_ident
                    """.trimIndent(),
                    paramMap = mapOf(
                        "person_ident" to ident,
                    ),
                ).map { row ->
                    val oppgaveId = row.uuid("id")
                    row.rehydrerOppgave(session.hentEmneknagger(oppgaveId))
                }.asList,
            )
        }
    }

    private fun TransactionalSession.lagre(oppgave: Oppgave) {
        this.run(
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
        oppgave.emneknagger.forEach { emneknagg ->
            this.run(
                queryOf(
                    //language=PostgreSQL
                    statement = """
                INSERT INTO emneknagg_v1(oppgave_id, emneknagg)
                VALUES (:oppgave_id, :emneknagg)
                    """.trimIndent(),
                    paramMap = mapOf<String, Any>(
                        "oppgave_id" to oppgave.oppgaveId,
                        "emneknagg" to emneknagg,
                    ),
                ).asUpdate,
            )
        }
    }

    override fun lagre(oppgave: Oppgave) {
        using(sessionOf(dataSource)) { session ->
            session.transaction { tx ->
                tx.lagre(oppgave)
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
                    row.rehydrerOppgave(session.hentEmneknagger(oppgaveId))
                }.asSingle,
            )
        }
    }

    private fun Row.rehydrerOppgave(emneknagger: Set<String>): Oppgave {
        return Oppgave.rehydrer(
            oppgaveId = this.uuid("id"),
            ident = this.string("person_ident"),
            opprettet = this.zonedDateTime("opprettet"),
            behandlingId = this.uuid("behandling_id"),
            emneknagger = emneknagger,
            tilstand = Oppgave.Tilstand.Type.valueOf(this.string("tilstand")),
        )
    }

    private fun Session.hentEmneknagger(oppgaveId: UUID): Set<String> {
        return this.run(
            queryOf(
                statement = """
                  SELECT emneknagg 
                  FROM emneknagg_v1
                  WHERE oppgave_id = :oppgave_id
                """.trimIndent(),
                paramMap = mapOf(
                    "oppgave_id" to oppgaveId,
                ),
            ).map { row -> row.string("emneknagg") }.asList,
        ).toSet()
    }

    override fun hentAlleOppgaver(): List<Oppgave> {
        return using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement = """
                    SELECT id,person_ident,opprettet,behandling_id,tilstand
                    FROM  oppgave_v1 
                    """.trimIndent(),
                ).map { row ->
                    val oppgaveId = row.uuid("id")
                    row.rehydrerOppgave(session.hentEmneknagger(oppgaveId))
                }.asList,
            )
        }
    }

    override fun hentAlleOppgaverMedTilstand(tilstand: Oppgave.Tilstand.Type): List<Oppgave> {
        return hentAlleOppgaver().filter { it.tilstand == tilstand }
    }
}
