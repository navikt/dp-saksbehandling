DO $$
DECLARE
    emneknagg RECORD;
BEGIN
    FOR emneknagg IN
        SELECT  DISTINCT(oppgave_id) as oppgave_id
        FROM    emneknagg_v1 emne
        WHERE   emne.emneknagg IN (
                'Minsteinntekt',
                'Arbeidsinntekt',
                'Arbeidstid',
                'Alder',
                'Andre ytelser',
                'Streik',
                'Opphold utland',
                'Reell arbeidssøker',
                'Ikke registrert',
                'Utestengt',
                'Utdanning',
                'Medlemskap'
                )
          AND NOT EXISTS (
              SELECT 1
              FROM   emneknagg_v1 emne2
              WHERE  emne2.oppgave_id = emne.oppgave_id
              AND    emne2.emneknagg = 'Avslag'
          )
    LOOP
        -- Sett inn ny emneknagg for Avslag
        INSERT INTO emneknagg_v1 (oppgave_id, emneknagg)
        VALUES (emneknagg.oppgave_id, 'Avslag' );
    END LOOP;
END $$;
