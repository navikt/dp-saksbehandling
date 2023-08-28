package no.nav.dagpenger.behandling

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import javax.sql.DataSource

interface PersonRepository {
    fun hentPerson(ident: String): Person?
    fun lagrePerson(person: Person)
}

object InMemoryPersonRepository : PersonRepository {
    private val personer = mutableSetOf<Person>()

    override fun hentPerson(ident: String): Person? {
        return personer.firstOrNull { it.ident == ident }
    }

    override fun lagrePerson(person: Person) {
        personer.add(person)
    }
}

class PostgresPersonRepository(private val dataSource: DataSource) : PersonRepository {

    override fun hentPerson(ident: String): Person? =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement = """SELECT ident FROM person WHERE ident = :ident""",
                    paramMap = mapOf("ident" to ident),
                ).map { row ->
                    Person(
                        row.string("ident"),
                    )
                }.asSingle,
            )
        }

    override fun lagrePerson(person: Person): Unit =
        using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement = """INSERT INTO person(ident) VALUES (:ident) ON CONFLICT DO NOTHING""",
                    paramMap = mapOf(
                        "ident" to person.ident,
                    ),
                ).asUpdate,
            )
        }
}
