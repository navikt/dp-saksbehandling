package no.nav.dagpenger.saksbehandling.db.generell

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.GenerellOppgaveData
import org.postgresql.util.PGobject
import java.util.UUID
import javax.sql.DataSource

class PostgresGenerellOppgaveDataRepository(
    private val dataSource: DataSource,
) : GenerellOppgaveDataRepository {
    companion object {
        private val objectMapper = ObjectMapper().registerKotlinModule()
    }

    override fun lagre(data: GenerellOppgaveData) {
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        INSERT INTO generell_oppgave_data_v1
                            (oppgave_id, oppgave_type, tittel, beskrivelse, strukturert_data)
                        VALUES
                            (:oppgave_id, :oppgave_type, :tittel, :beskrivelse, :strukturert_data)
                        ON CONFLICT (oppgave_id) DO UPDATE SET
                            oppgave_type = :oppgave_type,
                            tittel = :tittel,
                            beskrivelse = :beskrivelse,
                            strukturert_data = :strukturert_data
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "oppgave_id" to data.oppgaveId,
                            "oppgave_type" to data.oppgaveType,
                            "tittel" to data.tittel,
                            "beskrivelse" to data.beskrivelse,
                            "strukturert_data" to
                                data.strukturertData?.let {
                                    PGobject().also { pgObject ->
                                        pgObject.type = "JSONB"
                                        pgObject.value = objectMapper.writeValueAsString(it)
                                    }
                                },
                        ),
                ).asUpdate,
            )
        }
    }

    override fun hent(oppgaveId: UUID): GenerellOppgaveData? =
        sessionOf(dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        SELECT oppgave_id, oppgave_type, tittel, beskrivelse, strukturert_data
                        FROM generell_oppgave_data_v1
                        WHERE oppgave_id = :oppgave_id
                        """.trimIndent(),
                    paramMap = mapOf("oppgave_id" to oppgaveId),
                ).map { row ->
                    GenerellOppgaveData(
                        oppgaveId = row.uuid("oppgave_id"),
                        oppgaveType = row.string("oppgave_type"),
                        tittel = row.string("tittel"),
                        beskrivelse = row.stringOrNull("beskrivelse"),
                        strukturertData =
                            row.stringOrNull("strukturert_data")?.let {
                                objectMapper.readTree(it)
                            },
                    )
                }.asSingle,
            )
        }
}
