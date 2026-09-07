-- AvbruttHendelse er utvidet med årsak til avbrudd. Setter årsak til AVBRUTT_TRUKKET_KLAGE for alle eksisterende
-- AvbruttHendelse logger, siden de er brukt kun av endepunktet for å trekke klager.
-- Selve oppgaven skal ha tilstand AVBRUTT, ikke FERDIG_BEHANDLET. Skriptet retter også dette.
UPDATE  klage_tilstand_logg_v1
SET     hendelse =
        jsonb_set(
               hendelse::jsonb,
               '{årsak}',
               '"AVBRUTT_TRUKKET_KLAGE"'::jsonb,
               true
        )::json
WHERE   hendelse_type = 'AvbruttHendelse'
;

WITH oppgave AS
(
    SELECT  oppg.id
    FROM    oppgave_v1               oppg
    JOIN    oppgave_tilstand_logg_v1 logg ON logg.oppgave_id = oppg.id
    WHERE   logg.hendelse_type  = 'AvbruttHendelse'
)
UPDATE oppgave_v1 uopp
SET    tilstand = 'AVBRUTT'
FROM   oppgave
WHERE  oppgave.id = uopp.id
AND    tilstand != 'AVBRUTT'
;

UPDATE  oppgave_tilstand_logg_v1
SET     tilstand = 'AVBRUTT'
      , hendelse =
        jsonb_set(
               hendelse::jsonb,
               '{årsak}',
               '"AVBRUTT_TRUKKET_KLAGE"'::jsonb,
               true
        )::json
WHERE   hendelse_type = 'AvbruttHendelse'
;

DO $$
    DECLARE
        oppgave RECORD;
    BEGIN
        FOR oppgave IN
            SELECT  oppgave_id
            FROM    oppgave_tilstand_logg_v1 logg
            WHERE   logg.hendelse_type = 'AvbruttHendelse'
              AND NOT EXISTS (
                SELECT 1
                FROM   emneknagg_v1 emne
                WHERE  emne.oppgave_id = logg.oppgave_id
                AND    emne.emneknagg = 'Trukket klage'
            )
            LOOP
                -- Sett inn ny emneknagg for Trukket klage
                INSERT INTO emneknagg_v1 (oppgave_id, emneknagg)
                VALUES (oppgave.oppgave_id, 'Trukket klage' );
            END LOOP;
    END $$;
