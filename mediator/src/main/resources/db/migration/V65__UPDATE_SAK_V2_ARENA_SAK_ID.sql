ALTER TABLE sak_v2 DISABLE TRIGGER oppdater_endret_tidspunkt;

UPDATE sak_v2
SET    arena_sak_id = (
    SELECT utse.utsending_sak_id
    FROM   utsending_v1 utse
    JOIN   behandling_v1 beha ON beha.id = utse.behandling_id
    WHERE  utse.utsending_sak_id IS NOT NULL
    AND    beha.sak_id = sak_v2.id
    )
WHERE sak_v2.arena_sak_id IS NULL
AND EXISTS (
    SELECT 1
    FROM   utsending_v1 utse
    JOIN   behandling_v1 beha ON beha.id = utse.behandling_id
    WHERE  utse.utsending_sak_id IS NOT NULL
    AND    beha.sak_id = sak_v2.id
);

ALTER TABLE sak_v2 ENABLE TRIGGER oppdater_endret_tidspunkt;
