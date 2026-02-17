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
                            )
                            SELECT    log.id
                                    , CASE
                                        WHEN log.tilstand       = 'AVBRUTT' 
                                        AND  log.hendelse_type  = 'AvbrytOppgaveHendelse' THEN
                                            'AVBRUTT_MANUELT'
                                        ELSE
                                            log.tilstand
                                        END
                                    , log.tidspunkt
                                    , opp.id
                                    , opp.opprettet
                                    , beh.sak_id
                                    , beh.id
                                    , per.ident
                                    , CASE
                                        WHEN log.tilstand = 'UNDER_BEHANDLING' THEN 
                                            log.hendelse->>'ansvarligIdent'
                                        END
                                    , CASE 
                                        WHEN log.tilstand = 'UNDER_KONTROLL'   THEN 
                                            log.hendelse->>'ansvarligIdent' 
                                        END
                                    , beh.utlost_av
                                    , ins.resultat_type
                                    , CASE
                                        WHEN log.tilstand       = 'AVBRUTT' 
                                        AND  log.hendelse_type  = 'AvbrytOppgaveHendelse' THEN
                                            log.hendelse->>'årsak' 
                                        END
                                    , CASE WHEN log.tilstand = 'FERDIG_BEHANDLET' THEN
                                        CASE
                                            WHEN sak.er_dp_sak THEN
                                                'DAGPENGER'
                                            WHEN sak.arena_sak_id IS NOT NULL THEN
                                                'ARENA'
                                            ELSE
                                                'UKJENT'
                                        END
                                      END
                                    , sak.arena_sak_id
                            FROM      oppgave_tilstand_logg_v1      log
                            JOIN      oppgave_v1                    opp ON opp.id = log.oppgave_id
                            JOIN      behandling_v1                 beh ON beh.id = opp.behandling_id
                            JOIN      sak_v2                        sak ON sak.id = beh.sak_id
                            JOIN      person_v1                     per ON per.id = beh.person_id
                            LEFT JOIN innsending_v1                 ins ON ins.id = beh.id
                            WHERE     beh.utlost_av != 'KLAGE'
                            AND       log.id > coalesce((   SELECT      tilstand_id
                                                            FROM        saksbehandling_statistikk_v1
                                                            WHERE       overfort_til_statistikk = TRUE
                                                            ORDER BY    tilstand_id DESC
                                                            LIMIT 1
                                                        ) , '0198cc73-16cb-7a6b-ba93-f344c11d7922')
                            ORDER BY  log.id
                            LIMIT 100
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
