package no.nav.dagpenger.behandling

import kotliquery.action.UpdateQueryAction
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.dagpenger.behandling.oppgave.Oppgave
import no.nav.dagpenger.behandling.oppgave.OppgaveRepository
import java.util.UUID
import javax.sql.DataSource

class PostgresRepository(private val ds: DataSource) : PersonRepository, OppgaveRepository {

    override fun hentPerson(ident: String): Person? =
        using(sessionOf(ds)) { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement = """SELECT ident FROM person WHERE ident = :ident""",
                    paramMap = mapOf("ident" to ident),
                ).map { row ->
                    val funnetIdent = row.string("ident")
                    Person.rehydrer(funnetIdent, hentSakerFor(funnetIdent))
                }.asSingle,
            )
        }

    override fun lagrePerson(person: Person): Unit =
        using(sessionOf(ds)) { session ->
            val queries = LagrePersonStatementBuilder(person).queries
            session.transaction { tx ->
                queries.forEach {
                    tx.run(it)
                }
            }
        }

    private fun hentSakerFor(ident: String): Set<Sak> {
        return using(sessionOf(ds)) { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement = """SELECT uuid FROM sak WHERE person_ident = :person_ident""",
                    paramMap = mapOf("person_ident" to ident),
                ).map { row ->
                    Sak(
                        row.uuid("uuid"),
                    )
                }.asList,
            )
        }.toSet()
    }

    internal fun hentBehandling(uuid: UUID): Behandling {
        return using(sessionOf(ds)) { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement = """SELECT id, person_ident, opprettet, uuid, tilstand, sak_id FROM behandling WHERE uuid= :uuid""",
                    paramMap = mapOf("uuid" to uuid),
                ).map { row ->
                    val person =
                        hentPerson(row.string("person_ident")) ?: throw NotFoundException("Person ikke funnet")
                    Behandling.rehydrer(
                        person = person,
                        steg = setOf(), // todo implement
                        opprettet = row.localDateTime("opprettet"),
                        uuid = row.uuid("uuid"),
                        tilstand = row.string("tilstand"),
                        behandler = listOf(), // todo implement
                        sak = Sak(UUID.randomUUID()),
                    )
                }.asSingle,
            ) ?: throw NotFoundException("Behandling ikke funnet: $uuid")
        }
    }

    internal class NotFoundException(msg: String) : RuntimeException(msg)

    internal fun lagreBehandling(behandling: Behandling) {
        using(sessionOf(ds)) { session ->
            session.transaction { tx ->
                behandlingInsertStatementBuilder(behandling).forEach {
                    tx.run(it)
                }
            }
        }
    }

    override fun lagreOppgave(oppgave: Oppgave) {
        TODO("Not yet implemented")
    }

    override fun hentOppgave(uuid: UUID): Oppgave {
        TODO("Not yet implemented")
    }

    override fun hentOppgaver(): List<Oppgave> {
        TODO("Not yet implemented")
    }

    override fun hentOppgaverFor(fnr: String): List<Oppgave> {
        TODO("Not yet implemented")
    }

    private fun behandlingInsertStatementBuilder(behandling: Behandling): List<UpdateQueryAction> {
        //language=PostgreSQL
        val s1 = queryOf(
            statement = """
               INSERT INTO behandling(person_ident, opprettet, uuid, tilstand, sak_id)
               VALUES (:person_ident, :opprettet, :uuid, :tilstand, :sak_id)
               ON CONFLICT(uuid) DO UPDATE SET tilstand = :tilstand

            """.trimIndent(),
            paramMap = mapOf(
                "person_ident" to behandling.person.ident,
                "opprettet" to behandling.opprettet,
                "uuid" to behandling.uuid,
                "tilstand" to behandling.tilstand.javaClass.simpleName,
                "sak_id" to behandling.sak.id,
            ),
        ).asUpdate

        val s2:

        return listOf(s1)
    }
}

class LagrePersonStatementBuilder(private val person: Person) : PersonVisitor {
    val queries = mutableListOf<UpdateQueryAction>().also {
        it.add(
            queryOf(
                //language=PostgreSQL
                statement = """INSERT INTO person(ident) VALUES (:ident) ON CONFLICT DO NOTHING""",
                paramMap = mapOf(
                    "ident" to person.ident,
                ),
            ).asUpdate,
        )
    }

    init {
        person.accept(this)
    }

    override fun visit(saker: Set<Sak>) {
        saker.forEach { sak ->
            queries.add(
                queryOf(
                    //language=PostgreSQL
                    statement = """INSERT INTO sak(uuid, person_ident) VALUES (:uuid, :person_ident) ON CONFLICT DO NOTHING""",
                    paramMap = mapOf(
                        "uuid" to sak.id,
                        "person_ident" to person.ident,
                    ),
                ).asUpdate,
            )
        }
    }
}
