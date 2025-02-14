package no.nav.dagpenger.saksbehandling.statistikk

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.api.models.BeholdningsInfoDTO
import no.nav.dagpenger.saksbehandling.api.models.StatistikkDTO
import javax.sql.DataSource

interface StatistikkTjeneste {
    fun hentSaksbehandlerStatistikk(navIdent: String): StatistikkDTO

    fun hentAntallVedtakGjort(): StatistikkDTO

    fun hentBeholdningsInfo(): BeholdningsInfoDTO

    fun hentAntallBrevSendt(): Int
}

class PostgresStatistikkTjeneste(private val dataSource: DataSource) : StatistikkTjeneste {
    override fun hentSaksbehandlerStatistikk(navIdent: String): StatistikkDTO {
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
                    StatistikkDTO(
                        dag = row.int("dag"),
                        uke = row.int("uke"),
                        totalt = row.int("total"),
                    )
                }.asSingle,
            )
        } ?: StatistikkDTO()
    }

    override fun hentAntallVedtakGjort(): StatistikkDTO {
        return sessionOf(dataSource = dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement = """
                    SELECT
                    -- Count for the current day
                        (SELECT COUNT(*)
                         FROM oppgave_v1
                         WHERE tilstand = 'FERDIG_BEHANDLET'
                         AND endret_tidspunkt >= CURRENT_DATE
                         AND endret_tidspunkt < CURRENT_DATE + INTERVAL '1 day') AS dag,
    
                        -- Count for the current week
                        (SELECT COUNT(*)
                         FROM oppgave_v1
                         WHERE tilstand = 'FERDIG_BEHANDLET'
                         AND endret_tidspunkt >= date_trunc('week', CURRENT_DATE)
                         AND endret_tidspunkt < date_trunc('week', CURRENT_DATE) + INTERVAL '1 week') AS uke,
                         
                        -- Total count
                        (SELECT COUNT(*)
                         FROM oppgave_v1
                         WHERE tilstand = 'FERDIG_BEHANDLET') AS total;
                        
                    """,
                    paramMap = mapOf(),
                ).map { row ->
                    StatistikkDTO(
                        dag = row.int("dag"),
                        uke = row.int("uke"),
                        totalt = row.int("total"),
                    )
                }.asSingle,
            )
        } ?: StatistikkDTO()
    }

    override fun hentBeholdningsInfo(): BeholdningsInfoDTO {
        sessionOf(dataSource = dataSource).use { session ->
            return session.run(
                queryOf(
                    //language=PostgreSQL
                    statement = """
                        SELECT
                        -- Klar til behandling
                        (SELECT COUNT(*)
                         FROM oppgave_v1
                         WHERE tilstand = 'KLAR_TIL_BEHANDLING') AS klar_til_behandling,
                    
                        -- Klar til kontroll
                        (SELECT COUNT(*)
                         FROM oppgave_v1
                         WHERE tilstand = 'KLAR_TIL_KONTROLL') AS klar_til_kontroll,
                         
                        (SELECT min(opprettet)
                         FROM oppgave_v1
                         WHERE tilstand in ('KLAR_TIL_BEHANDLING','UNDER_BEHANDLING', 'KLAR_TIL_KONTROLL', 'UNDER_KONTROLL' ,'PAA_VENT')) AS eldste_dato
                    
                    """,
                    paramMap = mapOf(),
                ).map { row ->
                    BeholdningsInfoDTO(
                        antallOppgaverKlarTilBehandling = row.int("klar_til_behandling"),
                        antallOppgaverKlarTilKontroll = row.int("klar_til_kontroll"),
                        datoEldsteUbehandledeOppgave = row.localDateTime("eldste_dato"),
                    )
                }.asSingle,
            ) ?: BeholdningsInfoDTO()
        }
    }

    override fun hentAntallBrevSendt(): Int {
        return sessionOf(dataSource = dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement = """
                        select  count(*)  as count from utsending_v1 where  tilstand =  'Distribuert' ;
                    """,
                    paramMap = mapOf(),
                ).map { row ->
                    row.int("count")
                }.asSingle,
            )
        } ?: 0
    }
}
