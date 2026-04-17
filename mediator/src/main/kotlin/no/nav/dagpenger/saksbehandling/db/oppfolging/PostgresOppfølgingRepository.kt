package no.nav.dagpenger.saksbehandling.db.oppfolging

import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.db.oppgave.DataNotFoundException
import no.nav.dagpenger.saksbehandling.oppfolging.Oppfølging
import no.nav.dagpenger.saksbehandling.serder.objectMapper
import java.util.UUID
import javax.sql.DataSource

class PostgresOppfølgingRepository(
    private val dataSource: DataSource,
) : OppfølgingRepository {
    override fun lagre(oppfølging: Oppfølging) {
        sessionOf(dataSource).use { session ->
            session.transaction { tx ->
                val resultat = oppfølging.resultat()
                tx.run(
                    queryOf(
                        //language=PostgreSQL
                        statement =
                            """
                            INSERT INTO oppfolging_v1 (
                                id, 
                                person_id, 
                                tittel, 
                                beskrivelse,
                                strukturert_data,
                                frist,
                                opprettet, 
                                tilstand, 
                                vurdering,
                                resultat_type, 
                                resultat_behandling_id,
                                valgt_sak_id
                            )
                            VALUES (
                                :id, 
                                :person_id, 
                                :tittel, 
                                :beskrivelse,
                                :strukturert_data::jsonb,
                                :frist,
                                :opprettet, 
                                :tilstand, 
                                :vurdering,
                                :resultat_type, 
                                :resultat_behandling_id,
                                :valgt_sak_id
                            )
                            ON CONFLICT (id) 
                            DO UPDATE 
                            SET tilstand = :tilstand,
                                vurdering = :vurdering,
                                resultat_type = :resultat_type,
                                resultat_behandling_id = :resultat_behandling_id,
                                valgt_sak_id = :valgt_sak_id
                            """.trimIndent(),
                        paramMap =
                            mapOf(
                                "id" to oppfølging.id,
                                "person_id" to oppfølging.person.id,
                                "tittel" to oppfølging.tittel,
                                "beskrivelse" to oppfølging.beskrivelse,
                                "strukturert_data" to
                                    if (oppfølging.strukturertData.isEmpty()) {
                                        null
                                    } else {
                                        objectMapper.writeValueAsString(oppfølging.strukturertData)
                                    },
                                "frist" to oppfølging.frist,
                                "opprettet" to oppfølging.opprettet,
                                "tilstand" to oppfølging.tilstand(),
                                "vurdering" to oppfølging.vurdering(),
                                "valgt_sak_id" to oppfølging.valgtSakId(),
                                "resultat_type" to resultat.javaClass.simpleName,
                                "resultat_behandling_id" to
                                    when (resultat) {
                                        Oppfølging.Resultat.Ingen -> null
                                        is Oppfølging.Resultat.Klage -> resultat.behandlingId
                                        is Oppfølging.Resultat.RettTilDagpenger -> resultat.behandlingId
                                        is Oppfølging.Resultat.Oppfølging -> resultat.behandlingId
                                    },
                            ),
                    ).asUpdate,
                )
            }
        }
    }

    override fun hent(id: UUID): Oppfølging = finn(id) ?: throw DataNotFoundException("Kan ikke finne oppfølging med id $id")

    override fun finn(id: UUID): Oppfølging? {
        sessionOf(dataSource).use { session ->
            return session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        SELECT  go.id,
                                go.tittel,
                                go.beskrivelse,
                                go.strukturert_data,
                                go.frist,
                                go.opprettet,
                                go.tilstand,
                                go.vurdering,
                                go.resultat_type,
                                go.resultat_behandling_id,
                                go.valgt_sak_id,
                                p.id as person_id,
                                p.ident,
                                p.skjermes_som_egne_ansatte,
                                p.adressebeskyttelse_gradering as adressebeskyttelse
                        FROM    oppfolging_v1 go
                        JOIN    person_v1 p ON p.id = go.person_id
                        WHERE   go.id = :id
                        """.trimIndent(),
                    paramMap = mapOf("id" to id),
                ).map { row -> row.oppfølging() }.asSingle,
            )
        }
    }

    override fun finnForPerson(ident: String): List<Oppfølging> {
        sessionOf(dataSource).use { session ->
            return session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        SELECT  go.id,
                                go.tittel,
                                go.beskrivelse,
                                go.strukturert_data,
                                go.frist,
                                go.opprettet,
                                go.tilstand,
                                go.vurdering,
                                go.resultat_type,
                                go.resultat_behandling_id,
                                go.valgt_sak_id,
                                p.id as person_id,
                                p.ident,
                                p.skjermes_som_egne_ansatte,
                                p.adressebeskyttelse_gradering as adressebeskyttelse
                        FROM    oppfolging_v1 go
                        JOIN    person_v1 p ON p.id = go.person_id
                        WHERE   p.ident = :ident
                        """.trimIndent(),
                    paramMap = mapOf("ident" to ident),
                ).map { row -> row.oppfølging() }.asList,
            )
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun Row.oppfølging(): Oppfølging =
        Oppfølging.rehydrer(
            id = this.uuid("id"),
            person =
                Person(
                    id = this.uuid("person_id"),
                    ident = this.string("ident"),
                    skjermesSomEgneAnsatte = this.boolean("skjermes_som_egne_ansatte"),
                    adressebeskyttelseGradering = AdressebeskyttelseGradering.valueOf(this.string("adressebeskyttelse")),
                ),
            tittel = this.string("tittel"),
            beskrivelse = this.stringOrNull("beskrivelse") ?: "",
            strukturertData =
                this.stringOrNull("strukturert_data")?.let {
                    objectMapper.readValue(it, Map::class.java) as Map<String, Any>
                } ?: emptyMap(),
            frist = this.localDateOrNull("frist"),
            opprettet = this.localDateTime("opprettet"),
            tilstand = this.string("tilstand"),
            vurdering = this.stringOrNull("vurdering"),
            valgtSakId = this.uuidOrNull("valgt_sak_id"),
            resultat =
                when (val resultatType = this.stringOrNull("resultat_type")) {
                    "Ingen" -> Oppfølging.Resultat.Ingen
                    "Klage" -> Oppfølging.Resultat.Klage(this.uuid("resultat_behandling_id"))
                    "RettTilDagpenger" -> Oppfølging.Resultat.RettTilDagpenger(this.uuid("resultat_behandling_id"))
                    "Oppfølging" -> Oppfølging.Resultat.Oppfølging(this.uuid("resultat_behandling_id"))
                    null -> Oppfølging.Resultat.Ingen
                    else -> throw IllegalStateException("Ukjent resultat_type: $resultatType")
                },
        )
}
