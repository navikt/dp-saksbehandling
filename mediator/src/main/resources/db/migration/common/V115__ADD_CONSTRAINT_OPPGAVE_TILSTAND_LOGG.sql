DELETE
FROM oppgave_tilstand_logg_v1 logg
WHERE EXISTS (
    SELECT      dobl.oppgave_id
              , dobl.tilstand
              , COUNT(*)
    FROM        oppgave_tilstand_logg_v1 dobl
    WHERE       dobl.oppgave_id = logg.oppgave_id
    AND         dobl.tilstand   = 'FERDIG_BEHANDLET'
    GROUP BY    dobl.oppgave_id
              , dobl.tilstand
    HAVING      COUNT(*) > 1
)
AND logg.tilstand       = 'FERDIG_BEHANDLET'
AND logg.hendelse_type  = 'VedtakFattetHendelse'
;

CREATE UNIQUE INDEX idx_1_avsluttende_tilstand_per_oppgave
ON oppgave_tilstand_logg_v1(oppgave_id, tilstand)
WHERE tilstand IN ('FERDIG_BEHANDLET', 'AVBRUTT', 'AVBRUTT_MASKINELT')
;
