UPDATE  klage_tilstand_logg_v1
SET     hendelse =
        jsonb_set(
               hendelse::jsonb,
               '{årsak}',
               '"TRUKKET_KLAGE"'::jsonb,
               true
        )::json
WHERE   tilstand      = 'AVBRUTT'
AND     hendelse_type = 'AvbruttHendelse'
;

WITH oppgave AS
(
    SELECT  oppg.id
    FROM    oppgave_v1               oppg
    JOIN    oppgave_tilstand_logg_v1 logg ON logg.oppgave_id = oppg.id
    WHERE   oppg.tilstand       = 'FERDIG_BEHANDLET'
    AND     logg.hendelse_type  = 'AvbruttHendelse'
)
UPDATE oppgave_v1 uopp
SET    tilstand = 'AVBRUTT'
FROM   oppgave
WHERE  oppgave.id = uopp.id
;

UPDATE  oppgave_tilstand_logg_v1
SET     tilstand = 'AVBRUTT'
      , hendelse =
        jsonb_set(
               hendelse::jsonb,
               '{årsak}',
               '"TRUKKET_KLAGE"'::jsonb,
               true
        )::json
WHERE   tilstand      = 'FERDIG_BEHANDLET'
AND     hendelse_type = 'AvbruttHendelse'
;
