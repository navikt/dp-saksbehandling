-- Siden det fantes eldre behandlinger som ikke ble fanget opp av skriptet V86__SLETT_DOBLE_AVBRUTT_TILSTANDSENDRINGER.sql,
-- fordi de hadde fått tilstand AVBRUTT vha andre hendelser enn 'AvbrytOppgaveHendelse' eller 'BehandlingAvbruttHendelse',
-- må vi nå slette duplikatene som har hendelse_type 'SkriptHendelse', som ble satt av skript V87__UPDATE_HENDELSE_TYPE_TIL_SKRIPT_HENDELSE.sql.
-- Dette skriptet sletter de nyeste duplikatene.
DELETE
FROM    oppgave_tilstand_logg_v1 log2
WHERE   tilstand        = 'AVBRUTT'
AND     hendelse_type   = 'SkriptHendelse'
AND     EXISTS (SELECT  1
                FROM    oppgave_tilstand_logg_v1 log1
                WHERE   log1.oppgave_id     = log2.oppgave_id
                AND     log1.tilstand       = 'AVBRUTT'
                AND     log1.hendelse_type  = 'SkriptHendelse'
                AND     log1.tidspunkt      < log2.tidspunkt
               )
;
