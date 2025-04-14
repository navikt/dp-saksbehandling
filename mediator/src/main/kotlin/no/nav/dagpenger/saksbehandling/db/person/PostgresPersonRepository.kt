package no.nav.dagpenger.saksbehandling.db.person

import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.adressebeskyttelse.AdressebeskyttelseRepository
import no.nav.dagpenger.saksbehandling.db.oppgave.DataNotFoundException
import no.nav.dagpenger.saksbehandling.skjerming.SkjermingRepository
import javax.sql.DataSource

class PostgresPersonRepository(private val datasource: DataSource) : PersonRepository, SkjermingRepository, AdressebeskyttelseRepository {
    override fun finnPerson(ident: String): Person? {
        sessionOf(datasource).use { session ->
            return session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        SELECT * 
                        FROM   person_v1
                        WHERE  ident = :ident
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "ident" to ident,
                        ),
                ).map { row ->
                    Person(
                        id = row.uuid("id"),
                        ident = row.string("ident"),
                        skjermesSomEgneAnsatte = row.boolean("skjermes_som_egne_ansatte"),
                        adressebeskyttelseGradering = row.adresseBeskyttelseGradering(),
                    )
                }.asSingle,
            )
        }
    }

    override fun hentPerson(ident: String) = finnPerson(ident) ?: throw DataNotFoundException("Kan ikke finne person med ident $ident")

    override fun lagre(person: Person) {
        sessionOf(datasource).use { session ->
            session.transaction { tx ->
                tx.lagre(person)
            }
        }
    }

    override fun oppdaterSkjermingStatus(
        fnr: String,
        skjermet: Boolean,
    ): Int {
        return sessionOf(datasource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        UPDATE person_v1
                        SET    skjermes_som_egne_ansatte = :skjermet
                        WHERE  ident = :fnr
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "fnr" to fnr,
                            "skjermet" to skjermet,
                        ),
                ).asUpdate,
            )
        }
    }

    override fun oppdaterAdressebeskyttetStatus(
        fnr: String,
        adresseBeskyttelseGradering: AdressebeskyttelseGradering,
    ): Int {
        return sessionOf(datasource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        UPDATE person_v1
                        SET    adressebeskyttelse_gradering = :adresseBeskyttelseGradering
                        WHERE  ident = :fnr
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "fnr" to fnr,
                            "adresseBeskyttelseGradering" to adresseBeskyttelseGradering.name,
                        ),
                ).asUpdate,
            )
        }
    }

    override fun eksistererIDPsystem(fnrs: Set<String>): Set<String> {
        val identer = fnrs.joinToString { "'$it'" }
        return sessionOf(datasource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        SELECT ident
                        FROM   person_v1
                        WHERE  ident IN ($identer)
                        """.trimIndent(),
                ).map { row ->
                    row.string("ident")
                }.asList,
            ).toSet()
        }
    }
}

private fun TransactionalSession.lagre(person: Person) {
    run(
        queryOf(
            //language=PostgreSQL
            statement =
                """
                INSERT INTO person_v1
                    (id, ident, skjermes_som_egne_ansatte, adressebeskyttelse_gradering) 
                VALUES
                    (:id, :ident, :skjermes_som_egne_ansatte, :adressebeskyttelse_gradering) 
                ON CONFLICT (id) DO UPDATE SET skjermes_som_egne_ansatte = :skjermes_som_egne_ansatte , adressebeskyttelse_gradering = :adressebeskyttelse_gradering             
                """.trimIndent(),
            paramMap =
                mapOf(
                    "id" to person.id,
                    "ident" to person.ident,
                    "skjermes_som_egne_ansatte" to person.skjermesSomEgneAnsatte,
                    "adressebeskyttelse_gradering" to person.adressebeskyttelseGradering.name,
                ),
        ).asUpdate,
    )
}

private fun Row.adresseBeskyttelseGradering(): AdressebeskyttelseGradering {
    return AdressebeskyttelseGradering.valueOf(this.string("adressebeskyttelse_gradering"))
}
