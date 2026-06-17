DELETE
FROM    oppgave_tilstand_logg_v1 log1
WHERE   log1.tilstand      = 'AVBRUTT'
AND     log1.hendelse_type = 'BehandlingAvbruttHendelse'
AND EXISTS (
    SELECT  1
    FROM    oppgave_tilstand_logg_v1 log2
    WHERE   log2.oppgave_id     = log1.oppgave_id
    AND     log2.tilstand       = 'AVBRUTT'
    AND     log2.hendelse_type  = 'AvbrytOppgaveHendelse'
);