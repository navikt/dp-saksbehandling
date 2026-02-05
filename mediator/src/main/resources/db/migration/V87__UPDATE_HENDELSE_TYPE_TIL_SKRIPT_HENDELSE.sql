-- Tilstandslogg-innslag som har fått oppdatert tilstand til AVBRUTT vha skript, burde også hatt oppdatert hendelsetype/hendelse til 'SkriptHendelse'.
-- Dette skriptet oppdaterer hendelsetype for slike tilstandslogg-innslag.
UPDATE  oppgave_tilstand_logg_v1 log
SET     hendelse_type = 'SkriptHendelse',
        hendelse      = '{"utførtAv": "V87__UPDATE_HENDELSE_TYPE_TIL_SKRIPT_HENDELSE.sql"}'
WHERE   log.tilstand  = 'AVBRUTT'
AND     log.hendelse_type NOT IN ('AvbrytOppgaveHendelse','BehandlingAvbruttHendelse')
;
