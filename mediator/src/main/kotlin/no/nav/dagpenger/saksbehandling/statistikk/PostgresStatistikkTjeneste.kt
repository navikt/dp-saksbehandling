package no.nav.dagpenger.saksbehandling.statistikk

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.Configuration
import no.nav.dagpenger.saksbehandling.api.models.BeholdningsInfoDTO
import no.nav.dagpenger.saksbehandling.api.models.StatistikkDTO
import java.util.UUID
import javax.sql.DataSource

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

    override fun tidligereTilstandsendringErOverført(): Boolean {
        sessionOf(dataSource = dataSource).use { session ->
            val count =
                session.run(
                    queryOf(
                        //language=PostgreSQL
                        statement = """
                        SELECT COUNT(*) as count
                        FROM   saksbehandling_statistikk_v1
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

//                            AND     log.id > (
//                                    SELECT  coalesce(max(tilstand_id), '01992833-79c9-7257-85c2-0c382c7a2afe') AS siste_overforte_tilstandsendring
    override fun oppgaveTilstandsendringer(): List<OppgaveTilstandsendring> =
        sessionOf(dataSource = dataSource)
            .use { session ->
                session.run(
                    queryOf(
                        //language=PostgreSQL
                        statement = """
                        INSERT
                        INTO  saksbehandling_statistikk_v1 (
                              tilstand_id
                            , tilstand
                            , tilstand_tidspunkt
                            , oppgave_id
                            , mottatt
                            , sak_id
                            , behandling_id
                            , person_ident
                            , saksbehandler_ident
                            , beslutter_ident
                            , utlost_av               
                            )
                            SELECT    log.id
                                    , log.tilstand
                                    , log.tidspunkt
                                    , opp.id
                                    , opp.opprettet
                                    , beh.sak_id
                                    , beh.id
                                    , per.ident
                                    , CASE WHEN log.tilstand = 'UNDER_BEHANDLING' THEN log.hendelse->>'ansvarligIdent' END
                                    , CASE WHEN log.tilstand = 'UNDER_KONTROLL'   THEN log.hendelse->>'ansvarligIdent' END
                                    , beh.utlost_av               
                            FROM    oppgave_tilstand_logg_v1      log
                            JOIN    oppgave_v1                    opp ON opp.id = log.oppgave_id
                            JOIN    behandling_v1                 beh ON beh.id = opp.behandling_id
                            JOIN    person_v1                     per ON per.id = beh.person_id
                            AND     log.tidspunkt > (   SELECT  coalesce(max(tilstand_tidspunkt), '1900-01-01t00:00:00'::timestamptz )
                                                        FROM    saksbehandling_statistikk_v1
                                                        WHERE   overfort_til_statistikk = TRUE
                                                    )
                            ORDER BY log.id DESC 
                        ON CONFLICT (tilstand_id) DO NOTHING 
                        RETURNING   *
                        """,
                    ).map { row ->
                        OppgaveTilstandsendring(
                            oppgaveId = row.uuid("oppgave_id"),
                            mottatt = row.localDate("mottatt"),
                            sakId = row.uuid("sak_id"),
                            behandlingId = row.uuid("behandling_id"),
                            personIdent = row.string("person_ident"),
                            saksbehandlerIdent = row.stringOrNull("saksbehandler_ident"),
                            beslutterIdent = row.stringOrNull("beslutter_ident"),
                            versjon = Configuration.versjon,
                            tilstandsendring =
                                OppgaveTilstandsendring.StatistikkOppgaveTilstandsendring(
                                    id = row.uuid("tilstand_id"),
                                    tilstand = row.string("tilstand"),
                                    tidspunkt = row.localDateTime("tilstand_tidspunkt"),
                                ),
                            utløstAv = row.string("utlost_av"),
                        )
                    }.asList,
                )
            }

    override fun markerTilstandsendringerSomOverført(tilstandId: UUID) {
        sessionOf(dataSource = dataSource).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement = """
                        UPDATE saksbehandling_statistikk_v1
                        SET    overfort_til_statistikk = TRUE
                        WHERE  tilstand_id = :tilstand_id
                        """,
                    paramMap =
                        mapOf(
                            "tilstand_id" to tilstandId,
                        ),
                ).asUpdate,
            )
        }
    }
}
