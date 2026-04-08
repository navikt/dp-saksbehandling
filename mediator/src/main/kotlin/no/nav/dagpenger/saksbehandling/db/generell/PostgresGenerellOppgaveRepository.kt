package no.nav.dagpenger.saksbehandling.db.generell

import com.fasterxml.jackson.databind.JsonNode
import io.github.oshai.kotlinlogging.KotlinLogging
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.AdressebeskyttelseGradering
import no.nav.dagpenger.saksbehandling.Person
import no.nav.dagpenger.saksbehandling.db.oppgave.DataNotFoundException
import no.nav.dagpenger.saksbehandling.generell.GenerellOppgave
import no.nav.dagpenger.saksbehandling.serder.objectMapper
import java.util.UUID
import javax.sql.DataSource

private val logger = KotlinLogging.logger {}

class PostgresGenerellOppgaveRepository(
    private val dataSource: DataSource,
) : GenerellOppgaveRepository {
    override fun lagre(generellOppgave: GenerellOppgave) {
        sessionOf(dataSource).use { session ->
            session.transaction { tx ->
                val resultat = generellOppgave.resultat()
                tx.run(
                    queryOf(
                        //language=PostgreSQL
                        statement =
                            """
                            INSERT INTO generell_oppgave_v1 (
                                id, 
                                person_id, 
                                tittel, 
                                beskrivelse,
                                strukturert_data,
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
                                "id" to generellOppgave.id,
                                "person_id" to generellOppgave.person.id,
                                "tittel" to generellOppgave.tittel,
                                "beskrivelse" to generellOppgave.beskrivelse,
                                "strukturert_data" to generellOppgave.strukturertData?.let { objectMapper.writeValueAsString(it) },
                                "opprettet" to generellOppgave.opprettet,
                                "tilstand" to generellOppgave.tilstand(),
                                "vurdering" to generellOppgave.vurdering(),
                                "valgt_sak_id" to generellOppgave.valgtSakId(),
                                "resultat_type" to resultat?.javaClass?.simpleName,
                                "resultat_behandling_id" to
                                    when (resultat) {
                                        GenerellOppgave.Resultat.Ingen -> null
                                        is GenerellOppgave.Resultat.Klage -> resultat.behandlingId
                                        is GenerellOppgave.Resultat.RettTilDagpenger -> resultat.behandlingId
                                        null -> null
                                    },
                            ),
                    ).asUpdate,
                )
            }
        }
    }

    override fun hent(id: UUID): GenerellOppgave = finn(id) ?: throw DataNotFoundException("Kan ikke finne generell oppgave med id $id")

    override fun finn(id: UUID): GenerellOppgave? {
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
                        FROM    generell_oppgave_v1 go
                        JOIN    person_v1 p ON p.id = go.person_id
                        WHERE   go.id = :id
                        """.trimIndent(),
                    paramMap = mapOf("id" to id),
                ).map { row -> row.generellOppgave() }.asSingle,
            )
        }
    }

    override fun finnForPerson(ident: String): List<GenerellOppgave> {
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
                        FROM    generell_oppgave_v1 go
                        JOIN    person_v1 p ON p.id = go.person_id
                        WHERE   p.ident = :ident
                        """.trimIndent(),
                    paramMap = mapOf("ident" to ident),
                ).map { row -> row.generellOppgave() }.asList,
            )
        }
    }

    private fun Row.generellOppgave(): GenerellOppgave =
        GenerellOppgave.rehydrer(
            id = this.uuid("id"),
            person =
                Person(
                    id = this.uuid("person_id"),
                    ident = this.string("ident"),
                    skjermesSomEgneAnsatte = this.boolean("skjermes_som_egne_ansatte"),
                    adressebeskyttelseGradering = AdressebeskyttelseGradering.valueOf(this.string("adressebeskyttelse")),
                ),
            tittel = this.string("tittel"),
            beskrivelse = this.stringOrNull("beskrivelse"),
            strukturertData = this.stringOrNull("strukturert_data")?.let { objectMapper.readValue(it, JsonNode::class.java) },
            opprettet = this.localDateTime("opprettet"),
            tilstand = this.string("tilstand"),
            vurdering = this.stringOrNull("vurdering"),
            valgtSakId = this.uuidOrNull("valgt_sak_id"),
            resultat =
                when (val resultatType = this.stringOrNull("resultat_type")) {
                    "Ingen" -> GenerellOppgave.Resultat.Ingen
                    "Klage" -> GenerellOppgave.Resultat.Klage(this.uuid("resultat_behandling_id"))
                    "RettTilDagpenger" -> GenerellOppgave.Resultat.RettTilDagpenger(this.uuid("resultat_behandling_id"))
                    else -> null
                },
        )
}
