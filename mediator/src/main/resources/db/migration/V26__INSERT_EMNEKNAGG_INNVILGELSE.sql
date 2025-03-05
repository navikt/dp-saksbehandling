DO $$
    DECLARE
        emneknagg RECORD;
    BEGIN
        FOR emneknagg IN
            SELECT  DISTINCT(oppgave_id) as oppgave_id
            FROM    emneknagg_v1 emne
            WHERE   emne.emneknagg IN (
                                       'Ordinær',
                                       'Permittert',
                                       'Permittert fisk',
                                       'Konkurs'
                )
              AND NOT EXISTS (
                SELECT 1
                FROM   emneknagg_v1 emne2
                WHERE  emne2.oppgave_id = emne.oppgave_id
                  AND    emne2.emneknagg = 'Innvilgelse'
            )
            LOOP
                -- Sett inn ny emneknagg for Innvilgelse
                INSERT INTO emneknagg_v1 (oppgave_id, emneknagg)
                VALUES (emneknagg.oppgave_id, 'Innvilgelse' );
            END LOOP;
    END $$;
