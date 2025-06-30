DROP TRIGGER IF EXISTS oppdater_endret_tidspunkt ON sak_v2;

ALTER TABLE sak_v2
ADD COLUMN opprettet2            TIMESTAMP WITHOUT TIME ZONE;

UPDATE sak_v2
SET    opprettet2            = opprettet::timestamp with time zone at time zone 'Europe/Oslo'
WHERE  1 = 1;

ALTER TABLE sak_v2 DROP COLUMN opprettet;

ALTER table sak_v2
RENAME COLUMN opprettet2 TO opprettet;

ALTER table sak_v2
ALTER COLUMN opprettet SET NOT NULL;

CREATE TRIGGER oppdater_endret_tidspunkt
    BEFORE UPDATE
    ON sak_v2
    FOR EACH ROW
EXECUTE FUNCTION oppdater_endret_tidspunkt();
