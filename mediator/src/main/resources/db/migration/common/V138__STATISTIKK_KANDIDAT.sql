CREATE TABLE IF NOT EXISTS statistikk_kandidat_v1
(
    tilstand_id          UUID                        NOT NULL PRIMARY KEY,
    vurdert              TIMESTAMP WITHOUT TIME ZONE,
    registrert_tidspunkt TIMESTAMP WITHOUT TIME ZONE DEFAULT timezone('Europe/Oslo'::text, current_timestamp)
);

CREATE INDEX IF NOT EXISTS idx_statistikk_kandidat_v1_ikke_vurdert
    ON statistikk_kandidat_v1 (tilstand_id)
    WHERE vurdert IS NULL;

