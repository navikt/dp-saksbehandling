package no.nav.dagpenger.saksbehandling.db.person

import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.adressebeskyttelse.AdressebeskyttelseRepository
import no.nav.dagpenger.saksbehandling.db.oppgave.DataNotFoundException
import no.nav.dagpenger.saksbehandling.skjerming.SkjermingRepository
import java.util.UUID
import javax.sql.DataSource

private val sikkerlogg = KotlinLogging.logger("tjenestekall")

class PostgresPersonRepository(private val dataSource: DataSource) :
    PersonRepository,
    SkjermingRepository,
    AdressebeskyttelseRepository {
    override fun finnPerson(ident: String): Person? {
        sikkerlogg.info { "Søker etter person med ident $ident" }
        sessionOf(dataSource).use { session ->
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
                    row.tilPerson()
                }.asSingle,
            )
        }
    }

    override fun finnPerson(id: UUID): Person? {
        sikkerlogg.info { "Søker etter person med id $id" }
        sessionOf(dataSource).use { session ->
            return session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        SELECT * 
                        FROM   person_v1
                        WHERE  id = :id
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "id" to id,
                        ),
                ).map { row ->
                    row.tilPerson()
                }.asSingle,
            )
        }
    }

    private fun Row.tilPerson(): Person {
        return Person(
            id = this.uuid("id"),
            ident = this.string("ident"),
            skjermesSomEgneAnsatte = this.boolean("skjermes_som_egne_ansatte"),
            adressebeskyttelseGradering = this.adresseBeskyttelseGradering(),
        )
    }

    override fun hentPerson(ident: String) = finnPerson(ident) ?: throw DataNotFoundException("Kan ikke finne person med ident $ident")

    override fun hentPerson(id: UUID) = finnPerson(id) ?: throw DataNotFoundException("Kan ikke finne person med id $id")

    override fun lagre(person: Person) {
        sessionOf(dataSource).use { session ->
            session.transaction { tx ->
                tx.lagre(person)
            }
        }
    }

    override fun oppdaterSkjermingStatus(
        fnr: String,
        skjermet: Boolean,
    ): Int {
        return sessionOf(dataSource).use { session ->
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

    override fun oppdaterAdressebeskyttelseGradering(
        fnr: String,
        adresseBeskyttelseGradering: AdressebeskyttelseGradering,
    ): Int {
        return sessionOf(dataSource).use { session ->
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
        return sessionOf(dataSource).use { session ->
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
