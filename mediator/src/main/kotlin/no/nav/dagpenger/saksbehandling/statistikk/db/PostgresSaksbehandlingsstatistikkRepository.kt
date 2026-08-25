package no.nav.dagpenger.saksbehandling.statistikk.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.dagpenger.saksbehandling.Configuration
import no.nav.dagpenger.saksbehandling.db.DatabaseSession
import no.nav.dagpenger.saksbehandling.statistikk.OppgaveITilstand
import no.nav.dagpenger.saksbehandling.statistikk.db.SaksbehandlingsstatistikkRepository.Companion.ANTALL_PER_KJØRING
import java.util.UUID

class PostgresSaksbehandlingsstatistikkRepository(
    private val databaseSession: DatabaseSession,
) : SaksbehandlingsstatistikkRepository {
    override fun oppgaveTilstandsendringerIkkeOverfort(antall: Int): List<OppgaveITilstand> =
        databaseSession.session { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    statement =
                        """
                        SELECT   *
                        FROM     saksbehandling_statistikk_v1
                        WHERE    overfort_til_statistikk = FALSE
                        ORDER BY sekvensnummer
                        LIMIT    :antall
                        """.trimIndent(),
                    paramMap = mapOf("antall" to antall),
                ).map { row ->
                    row.mapToOppgaveTilstand()
                }.asList,
            )
        }

    override fun oppgaveTilstandsendringer(): List<OppgaveITilstand> =
        databaseSession.transaction {
            val tilstandIder = session.hentKandidater(ANTALL_PER_KJØRING)
            when (tilstandIder.isEmpty()) {
                true -> {
                    emptyList()
                }

                false -> {
                    val oppgaveITilstander = session.skrivStatistikkrader(tilstandIder)
                    session.markerSomVurdert(tilstandIder)
                    oppgaveITilstander
                }
            }
        }

    private fun Session.hentKandidater(antall: Int): List<UUID> =
        this.run(
            queryOf(
                //language=PostgreSQL
                statement =
                    """
                    SELECT   tilstand_id
                    FROM     statistikk_kandidat_v1
                    WHERE    vurdert IS NULL
                    ORDER BY sekvensnummer
                    LIMIT    :antall
                    """.trimIndent(),
                paramMap = mapOf("antall" to antall),
            ).map { it.uuid("tilstand_id") }.asList,
        )

    private fun Session.markerSomVurdert(tilstandIder: List<UUID>): Int =
        this.run(
            queryOf(
                //language=PostgreSQL
                statement =
                    """
                    UPDATE statistikk_kandidat_v1
                    SET    vurdert     = timezone('Europe/Oslo'::text, current_timestamp)
                    WHERE  tilstand_id = ANY (:tilstand_ider)
                    """.trimIndent(),
                paramMap = mapOf("tilstand_ider" to createArrayOf("uuid", tilstandIder)),
            ).asUpdate,
        )

    // Går ikke lenger tilbake i tid enn det finnes behandlinger i behandlinger_mart på BigQuery, derfor begrensningen
    // på beh.id >= '019928dc-f521-7723-8ff6-f07154f5097d' (som er den første behandlingen i behandlinger_mart).
    // For å få med all historikken på første klage som inkluderes, ekskluderes klager med id
    // 01a01292-a2da-70a7-9c0c-d0ddc1db3888 eller eldre. Avgrensningen står i WHERE og ikke som betingelse på
    // LEFT JOIN klage_v1: en LEFT JOIN beholder loggraden uansett om betingelsen slår til, så klagen ville
    // blitt eksportert med tomt utfall og endt som UKJENT i datavarehuset i stedet for å bli holdt utenfor.
    private fun Session.skrivStatistikkrader(tilstandIder: List<UUID>): List<OppgaveITilstand> =
        this.run(
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
                            , relatert_behandling_id
                            )
                            SELECT    log.id                    AS tilstand_id
                                    , CASE
                                        WHEN log.tilstand       = 'AVBRUTT' 
                                        AND  log.hendelse_type  = 'AvbrytOppgaveHendelse' THEN
                                            'AVBRUTT_MANUELT'
                                        WHEN log.tilstand       = 'UNDER_BEHANDLING'
                                        AND  log.hendelse_type  = 'ReturnerTilSaksbehandlingHendelse' THEN
                                            'UNDERKJENT_BESLUTTER'
                                        WHEN log.tilstand       = 'UNDER_BEHANDLING'
                                        AND  log.hendelse_type  = 'BehandlingTilGodkjenningHendelse' THEN
                                            'RETURNERT_MASKINELT'
                                        WHEN log.tilstand       = 'FERDIG_BEHANDLET'
                                        AND  log.hendelse_type  = 'KlageBehandlingUtført'
                                        AND  klage_utfall -> 'verdi' ->> 'value' = 'Opprettholdelse' THEN
                                            'OVERSENDT_KLAGEINSTANS'
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
                                    , CASE
                                        WHEN beh.utlost_av = 'INNSENDING' THEN
                                            ins.resultat_type
                                        WHEN beh.utlost_av = 'KLAGE'
                                        AND  log.hendelse_type = 'KlageBehandlingUtført' THEN
                                            klage_utfall -> 'verdi' ->> 'value'
                                        WHEN beh.utlost_av = 'KLAGE'
                                        AND  log.hendelse_type = 'KlageinstansVedtakHendelse' THEN
                                            log.hendelse ->> 'utfall'
                                        END                     AS behandling_resultat
                                    , CASE
                                        WHEN log.hendelse_type  = 'UtsettOppgaveHendelse' THEN
                                            log.hendelse ->> 'årsak' 
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
                                        WHEN log.hendelse_type IN ('ReturnerTilSaksbehandlingHendelse','AvbrytOppgaveHendelse') THEN 
                                            log.hendelse->>'årsak'
                                        END                     AS resultat_begrunnelse
                                    , (paaklaget_vedtak -> 'verdi' ->> 'value')::UUID AS relatert_behandling_id
                            FROM      oppgave_tilstand_logg_v1      log
                            JOIN      oppgave_v1                    opp ON opp.id = log.oppgave_id
                            JOIN      behandling_v1                 beh ON beh.id = opp.behandling_id
                            JOIN      sak_v2                        sak ON sak.id = beh.sak_id
                            JOIN      person_v1                     per ON per.id = beh.person_id
                            JOIN      statistikk_kandidat_v1        sta ON sta.tilstand_id = log.id
                            LEFT JOIN innsending_v1                 ins ON ins.id = beh.id
                            LEFT JOIN klage_v1                      kla ON kla.id = beh.id
                                                                        AND kla.id > '01a01292-a2da-70a7-9c0c-d0ddc1db3888'
                            LEFT JOIN LATERAL jsonb_array_elements(kla.opplysninger) klage_utfall
                                ON  klage_utfall    ->> 'type' = 'UTFALL'
                            LEFT JOIN LATERAL jsonb_array_elements(kla.opplysninger) paaklaget_vedtak
                                ON  paaklaget_vedtak ->> 'type' = 'KLAGEN_GJELDER_VEDTAK'
                            WHERE     beh.id >= '019928dc-f521-7723-8ff6-f07154f5097d'
                            AND       log.id = ANY (:tilstand_ider)
                            ORDER BY  sta.sekvensnummer
                        RETURNING   *
                        """,
                paramMap = mapOf("tilstand_ider" to createArrayOf("uuid", tilstandIder)),
            ).map { row ->
                row.mapToOppgaveTilstand()
            }.asList,
        )

    private fun Row.mapToOppgaveTilstand(): OppgaveITilstand =
        OppgaveITilstand(
            oppgaveId = this.uuid("oppgave_id"),
            mottatt = this.localDateTime("mottatt"),
            sakId = this.uuid("sak_id"),
            behandlingId = this.uuid("behandling_id"),
            personIdent = this.string("person_ident"),
            saksbehandlerIdent = this.stringOrNull("saksbehandler_ident"),
            beslutterIdent = this.stringOrNull("beslutter_ident"),
            versjon = Configuration.versjon,
            tilstandsendring =
                OppgaveITilstand.Tilstandsendring(
                    sekvensnummer = this.long("sekvensnummer"),
                    tilstandsendringId = this.uuid("tilstand_id"),
                    tilstand = this.string("tilstand"),
                    tidspunkt = this.localDateTime("tilstand_tidspunkt"),
                ),
            utløstAv = this.string("utlost_av"),
            behandlingResultat = this.stringOrNull("behandling_resultat"),
            behandlingÅrsak = this.stringOrNull("behandling_aarsak"),
            fagsystem = this.stringOrNull("fagsystem"),
            arenaSakId = this.stringOrNull("arena_sak_id"),
            resultatBegrunnelse = this.stringOrNull("resultat_begrunnelse"),
            relatertBehandlingId = this.uuidOrNull("relatert_behandling_id"),
        )

    override fun markerTilstandsendringerSomOverført(tilstandId: UUID): Int =
        databaseSession.session { session ->
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
