package no.nav.dagpenger.saksbehandling.db.innsending

import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.db.oppgave.DataNotFoundException
import no.nav.dagpenger.saksbehandling.hendelser.Kategori
import no.nav.dagpenger.saksbehandling.innsending.Innsending
import java.util.UUID
import javax.sql.DataSource

private val logger = KotlinLogging.logger {}
private val sikkerlogger = KotlinLogging.logger("tjenestekall")

class PostgresInnsendingRepository(private val dataSource: DataSource) : InnsendingRepository {
    override fun lagre(innsending: Innsending) {
        sessionOf(dataSource).use { session ->
            session.transaction { tx ->
                tx.run(
                    queryOf(
                        //language=PostgreSQL
                        statement =
                            """
                            INSERT INTO innsending_v1
                                (id, person_id, journalpost_id, skjema_kode, kategori, mottatt, soknad_id)
                            VALUES
                                (:id, :person_id, :journalpost_id, :skjema_kode, :kategori, :mottatt, :soknad_id)
                            ON CONFLICT (id) DO NOTHING 
                            """.trimIndent(),
                        paramMap =
                            mapOf(
                                "id" to innsending.innsendingId,
                                "person_id" to innsending.person.id,
                                "journalpost_id" to innsending.journalpostId,
                                "skjema_kode" to innsending.skjemaKode,
                                "kategori" to innsending.kategori.name,
                                "mottatt" to innsending.mottatt,
                                "soknad_id" to innsending.søknadId,
                            ),
                    ).asUpdate,
                )
            }
        }
    }

    override fun hent(innsendingId: UUID): Innsending {
        return finnInnsending(innsendingId)
            ?: throw DataNotFoundException("Kan ikke finne innsending med id $innsendingId")
    }

    override fun finnInnsendingerForPerson(ident: String): List<Innsending> {
        sessionOf(dataSource).use { session ->
            return session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        SELECT  inns.id as innsending_id, 
                                inns.journalpost_id, 
                                inns.skjema_kode, 
                                inns.kategori,
                                inns.mottatt, 
                                pers.id as person_id, 
                                pers.ident, 
                                pers.skjermes_som_egne_ansatte, 
                                pers.adressebeskyttelse_gradering as adressebeskyttelse
                        FROM    innsending_v1 inns
                        JOIN    person_v1 pers ON pers.id = inns.person_id
                        WHERE   pers.ident = :ident
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "ident" to ident,
                        ),
                ).map { row ->
                    row.innsending()
                }.asList,
            )
        }
    }

    private fun finnInnsending(innsendingId: UUID): Innsending? {
        sessionOf(dataSource).use { session ->
            return session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        SELECT  inns.id as innsending_id, 
                                inns.journalpost_id, 
                                inns.skjema_kode, 
                                inns.kategori,
                                inns.mottatt, 
                                pers.id as person_id, 
                                pers.ident, 
                                pers.skjermes_som_egne_ansatte, 
                                pers.adressebeskyttelse_gradering as adressebeskyttelse
                        FROM    innsending_v1 inns
                        JOIN    person_v1 pers ON pers.id = inns.person_id
                        WHERE   inns.id = :innsending_id
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "innsending_id" to innsendingId,
                        ),
                ).map { row ->
                    row.innsending()
                }.asSingle,
            )
        }
    }

    private fun Row.innsending(): Innsending {
        return Innsending.rehydrer(
            innsendingId = this.uuid("innsending_id"),
            person =
                Person(
                    id = this.uuid("person_id"),
                    ident = this.string("ident"),
                    skjermesSomEgneAnsatte = this.boolean("skjermes_som_egne_ansatte"),
                    adressebeskyttelseGradering = AdressebeskyttelseGradering.valueOf(this.string("adressebeskyttelse")),
                ),
            journalpostId = this.string("journalpost_id"),
            mottatt = this.localDateTime("mottatt"),
            skjemaKode = this.string("skjema_kode"),
            kategori = Kategori.valueOf(this.string("kategori")),
        )
    }
}
