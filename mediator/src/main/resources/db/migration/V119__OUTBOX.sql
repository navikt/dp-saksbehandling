CREATE TABLE IF NOT EXISTS kafka_utboks_v1
(
    id                   BIGSERIAL PRIMARY KEY,
    key                  TEXT NOT NULL,
    message              TEXT NOT NULL,
    status               TEXT NOT NULL,
    registrert_tidspunkt TIMESTAMP WITHOUT TIME ZONE DEFAULT timezone('Europe/Oslo'::text, current_timestamp),
    endret_tidspunkt     TIMESTAMP WITHOUT TIME ZONE DEFAULT timezone('Europe/Oslo'::text, current_timestamp)
);

CREATE INDEX IF NOT EXISTS idx_kafka_utboks_v1_pending ON kafka_utboks_v1 (id) WHERE status = 'PENDING';

CREATE OR REPLACE TRIGGER oppdater_endret_tidspunkt
    BEFORE UPDATE
    ON kafka_utboks_v1
    FOR EACH ROW
EXECUTE FUNCTION oppdater_endret_tidspunkt();

DO
$$
BEGIN
        IF EXISTS
            (SELECT 1 FROM pg_roles WHERE rolname = 'cloudsqliamuser')
        THEN
            GRANT SELECT ON TABLE kafka_utboks_v1 TO cloudsqliamuser;
END IF;
END
$$;
