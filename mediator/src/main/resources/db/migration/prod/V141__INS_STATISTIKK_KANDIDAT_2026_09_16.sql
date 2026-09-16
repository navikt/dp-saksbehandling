INSERT INTO statistikk_kandidat_v1
(
    tilstand_id
)
SELECT  id AS tilstand_id
FROM    oppgave_tilstand_logg_v1
WHERE   id = '01a03877-d04a-76d7-abac-4adf58bbb390'
;
