package no.nav.dagpenger.saksbehandling.statistikk

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.api.models.BeholdningsInfoDTO
import no.nav.dagpenger.saksbehandling.api.models.StatistikkDTO
import java.util.UUID
import javax.sql.DataSource

interface StatistikkTjeneste {
    fun hentSaksbehandlerStatistikk(navIdent: String): StatistikkDTO

    fun hentAntallVedtakGjort(): StatistikkDTO

    fun hentBeholdningsInfo(): BeholdningsInfoDTO

    fun hentAntallBrevSendt(): Int

    fun oppgaverTilStatistikk(): List<UUID>

    fun markerOppgaveTilStatistikkSomOverført(oppgaveId: UUID): Int

    fun tidligereOppgaverErOverførtTilStatistikk(): Boolean
}

class PostgresStatistikkTjeneste(
    private val dataSource: DataSource,
) : StatistikkTjeneste {
    override fun hentSaksbehandlerStatistikk(navIdent: String): StatistikkDTO =
        sessionOf(dataSource = dataSource).use { session ->
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
        } ?: StatistikkDTO(0, 0, 0)

    override fun hentAntallVedtakGjort(): StatistikkDTO =
        sessionOf(dataSource = dataSource).use { session ->
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
        } ?: StatistikkDTO(0, 0, 0)

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
            ) ?: BeholdningsInfoDTO(0, 0)
        }
    }

    override fun hentAntallBrevSendt(): Int =
        sessionOf(dataSource = dataSource).use { session ->
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

    override fun oppgaverTilStatistikk(): List<UUID> {
        sessionOf(dataSource = dataSource).use { session ->
            return session.run(
                queryOf(
                    //language=PostgreSQL
                    statement = """
                        INSERT
                        INTO    oppgave_til_statistikk_v1 (
                                oppgave_id,
                                ferdig_behandlet_tidspunkt
                            )
                            SELECT  oppgave.id,
                                    avslutt.tidspunkt
                            FROM    oppgave_v1                      oppgave
                            JOIN    behandling_v1                   behandl ON behandl.id = oppgave.behandling_id
                            JOIN    sak_v2                          sak     ON sak.id = behandl.sak_id
                            JOIN ( SELECT oppgave_id, MAX(tidspunkt) AS tidspunkt
                                   FROM   oppgave_tilstand_logg_v1
                                   WHERE  tilstand IN ( 'FERDIG_BEHANDLET', 'AVBRUTT')
                                   GROUP BY oppgave_id
                                 ) avslutt ON avslutt.oppgave_id = oppgave.id
                            
                            WHERE   oppgave.tilstand IN ( 'FERDIG_BEHANDLET', 'AVBRUTT')
                            AND     sak.er_dp_sak = TRUE
                            AND     avslutt.tidspunkt > (
                                SELECT  coalesce(
                                            max(ferdig_behandlet_tidspunkt), 
                                            '1900-01-01t00:00:00'::timestamptz
                                        ) AS siste_overforingstidspunkt
                                FROM    oppgave_til_statistikk_v1
                                WHERE   overfort_til_statistikk = TRUE
                                )
                            ORDER BY avslutt.tidspunkt 
                        ON CONFLICT (oppgave_id) DO UPDATE SET ferdig_behandlet_tidspunkt = EXCLUDED.ferdig_behandlet_tidspunkt
                        RETURNING   oppgave_id 
                    """,
                ).map { row ->
                    row.uuid("oppgave_id")
                }.asList,
            )
        }
    }

    override fun markerOppgaveTilStatistikkSomOverført(oppgaveId: UUID): Int =
        sessionOf(dataSource = dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement = """
                            UPDATE oppgave_til_statistikk_v1
                            SET    overfort_til_statistikk = TRUE
                            WHERE  oppgave_id = :oppgave_id
                            """,
                    paramMap =
                        mapOf(
                            "oppgave_id" to oppgaveId,
                        ),
                ).asUpdate,
            )
        }

    override fun tidligereOppgaverErOverførtTilStatistikk(): Boolean {
        sessionOf(dataSource = dataSource).use { session ->
            val count =
                session.run(
                    queryOf(
                        //language=PostgreSQL
                        statement = """
                        SELECT COUNT(*) as count
                        FROM   oppgave_til_statistikk_v1
                        WHERE  overfort_til_statistikk = FALSE;
                    """,
                        paramMap = mapOf(),
                    ).map { row ->
                        row.int("count")
                    }.asSingle,
                )
            return count == 0
        }
    }
}
