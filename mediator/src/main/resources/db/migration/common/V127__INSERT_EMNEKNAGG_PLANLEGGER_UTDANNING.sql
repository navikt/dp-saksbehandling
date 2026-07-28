INSERT INTO emneknagg_v1
    ( oppgave_id
    , emneknagg
    )
SELECT
      emne.oppgave_id
    , 'Planlegger utdanning' AS emneknagg
FROM emneknagg_v1 emne
WHERE emneknagg = 'Utdanning'
AND EXISTS (SELECT 1
            FROM oppgave_v1 oppg
            WHERE oppg.id = emne.oppgave_id
              AND oppg.tilstand = 'KLAR_TIL_BEHANDLING');
