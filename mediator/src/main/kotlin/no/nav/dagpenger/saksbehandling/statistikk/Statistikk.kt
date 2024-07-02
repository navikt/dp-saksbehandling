package no.nav.dagpenger.saksbehandling.statistikk

import kotliquery.queryOf
import kotliquery.sessionOf
import javax.sql.DataSource

interface StatistikkTjeneste {
    fun hentStatistikk(navIdent: String): Statistikk
}

data class Statistikk(
    val dag: Int = 0,
    val uke: Int = 0,
    val totalt: Int = 0,
)

class PostgresStatistikkTjeneste(private val dataSource: DataSource) : StatistikkTjeneste {
    override fun hentStatistikk(navIdent: String): Statistikk {
        return sessionOf(dataSource = dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement = """
                    WITH filtrert_data AS (SELECT *
                                           FROM oppgave_v1
                                           WHERE tilstand = 'FERDIG_BEHANDLET'
                                           AND saksbehandler_ident = :navIdent)
                    SELECT
                    -- Count for the current day
                        (SELECT COUNT(*)
                         FROM filtrert_data
                         WHERE endret_tidspunkt >= CURRENT_DATE
                         AND endret_tidspunkt < CURRENT_DATE + INTERVAL '1 day') AS dag,
    
                        -- Count for the current week
                        (SELECT COUNT(*)
                         FROM filtrert_data
                         WHERE endret_tidspunkt >= date_trunc('week', CURRENT_DATE)
                         AND endret_tidspunkt < date_trunc('week', CURRENT_DATE) + INTERVAL '1 week') AS uke,
                         
                        -- Total count
                        (SELECT COUNT(*) FROM filtrert_data) AS total;
                        
                    """,
                    paramMap = mapOf("navIdent" to navIdent),
                ).map { row ->
                    Statistikk(
                        dag = row.int("dag"),
                        uke = row.int("uke"),
                        totalt = row.int("total"),
                    )
                }.asSingle,
            )
        } ?: Statistikk()
    }
}
