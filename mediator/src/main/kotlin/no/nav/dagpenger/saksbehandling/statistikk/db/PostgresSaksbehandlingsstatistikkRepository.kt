package no.nav.dagpenger.saksbehandling.statistikk.db

import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.saksbehandling.Configuration
import no.nav.dagpenger.saksbehandling.statistikk.OppgaveITilstand
import java.util.UUID
import javax.sql.DataSource

class PostgresSaksbehandlingsstatistikkRepository(
    private val dataSource: DataSource,
) : SaksbehandlingsstatistikkRepository {
    override fun tidligereTilstandsendringerErOverført(): Boolean {
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

    // Henter oppgavetilstander som skal sendes til statistikk.
    // Går ikke lenger tilbake i tid enn det finnes behandlinger i behandlinger_mart på BigQuery, derfor begrensningen
    // på beh.id >= '019928dc-f521-7723-8ff6-f07154f5097d' (som er den første behandlingen i behandlinger_mart).
    override fun oppgaveTilstandsendringer(): List<OppgaveITilstand> =
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
                            , behandling_resultat
                            , behandling_aarsak
                            , fagsystem
                            , arena_sak_id
                            , resultat_begrunnelse
                            )
                            SELECT    log.id                    AS tilstand_id
                                    , CASE
                                        WHEN log.tilstand       = 'AVBRUTT' 
                                        AND  log.hendelse_type  = 'AvbrytOppgaveHendelse' THEN
                                            'AVBRUTT_MANUELT'
                                        ELSE
                                            log.tilstand
                                        END                     AS tilstand
                                    , log.tidspunkt             AS tilstand_tidspunkt
                                    , opp.id                    AS oppgave_id
                                    , opp.opprettet             AS mottatt
                                    , beh.sak_id                AS sak_id
                                    , beh.id                    AS behandling_id
                                    , per.ident                 AS person_ident
                                    , CASE
                                        WHEN log.tilstand = 'UNDER_BEHANDLING' THEN 
                                            log.hendelse->>'ansvarligIdent'
                                        END                     AS saksbehandler_ident
                                    , CASE 
                                        WHEN log.tilstand = 'UNDER_KONTROLL'   THEN 
                                            log.hendelse->>'ansvarligIdent' 
                                        END                     AS beslutter_ident
                                    , beh.utlost_av             AS utlost_av
                                    , ins.resultat_type         AS behandling_resultat
                                    , CASE
                                        WHEN log.tilstand       = 'AVBRUTT' 
                                        AND  log.hendelse_type  = 'AvbrytOppgaveHendelse' THEN
                                             log.hendelse->>'årsak' 
                                        END                     AS behandling_aarsak
                                    , CASE
                                        WHEN sak.er_dp_sak THEN
                                            'DAGPENGER'
                                        WHEN sak.arena_sak_id IS NOT NULL THEN
                                            'ARENA'
                                        ELSE
                                            'UKJENT'
                                        END                     AS fagsystem
                                    , sak.arena_sak_id          AS arena_sak_id
                                    , CASE
                                        WHEN log.hendelse_type IN ('UtsettOppgaveHendelse','ReturnerTilSaksbehandlingHendelse') THEN 
                                            log.hendelse->>'årsak'
                                        END                     AS resultat_begrunnelse 
                            FROM      oppgave_tilstand_logg_v1      log
                            JOIN      oppgave_v1                    opp ON opp.id = log.oppgave_id
                            JOIN      behandling_v1                 beh ON beh.id = opp.behandling_id
                            JOIN      sak_v2                        sak ON sak.id = beh.sak_id
                            JOIN      person_v1                     per ON per.id = beh.person_id
                            LEFT JOIN innsending_v1                 ins ON ins.id = beh.id
                            WHERE     beh.utlost_av != 'KLAGE'
                            AND       beh.id >= '019928dc-f521-7723-8ff6-f07154f5097d'
                            AND       log.id >  coalesce((  SELECT      tilstand_id
                                                            FROM        saksbehandling_statistikk_v1
                                                            WHERE       overfort_til_statistikk = TRUE
                                                            ORDER BY    tilstand_id DESC
                                                            LIMIT 1
                                                        ) , '0198cc73-16cb-7a6b-ba93-f344c11d7922')
                            ORDER BY  log.id
                            LIMIT 1000
                        RETURNING   *
                        """,
                    ).map { row ->
                        OppgaveITilstand(
                            oppgaveId = row.uuid("oppgave_id"),
                            mottatt = row.localDateTime("mottatt"),
                            sakId = row.uuid("sak_id"),
                            behandlingId = row.uuid("behandling_id"),
                            personIdent = row.string("person_ident"),
                            saksbehandlerIdent = row.stringOrNull("saksbehandler_ident"),
                            beslutterIdent = row.stringOrNull("beslutter_ident"),
                            versjon = Configuration.versjon,
                            tilstandsendring =
                                OppgaveITilstand.Tilstandsendring(
                                    sekvensnummer = row.long("sekvensnummer"),
                                    tilstandsendringId = row.uuid("tilstand_id"),
                                    tilstand = row.string("tilstand"),
                                    tidspunkt = row.localDateTime("tilstand_tidspunkt"),
                                ),
                            utløstAv = row.string("utlost_av"),
                            behandlingResultat = row.stringOrNull("behandling_resultat"),
                            behandlingÅrsak = row.stringOrNull("behandling_aarsak"),
                            fagsystem = row.stringOrNull("fagsystem"),
                            arenaSakId = row.stringOrNull("arena_sak_id"),
                            resultatBegrunnelse = row.stringOrNull("resultat_begrunnelse"),
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
