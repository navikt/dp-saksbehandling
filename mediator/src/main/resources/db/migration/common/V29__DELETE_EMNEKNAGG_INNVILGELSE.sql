DELETE
FROM  emneknagg_v1 innvilg
WHERE innvilg.emneknagg = 'Innvilgelse'
AND EXISTS (
    SELECT 1
    FROM  emneknagg_v1 avslag
    WHERE avslag.oppgave_id = innvilg.oppgave_id
    AND   avslag.emneknagg = 'Avslag');
