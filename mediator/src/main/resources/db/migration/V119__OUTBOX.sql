CREATE TABLE IF NOT EXISTS outbox
(
    id                   BIGSERIAL PRIMARY KEY,
    key                  TEXT NOT NULL,
    message              TEXT NOT NULL,
    status               TEXT NOT NULL,
    registrert_tidspunkt TIMESTAMP WITHOUT TIME ZONE DEFAULT timezone('Europe/Oslo'::text, current_timestamp),
    endret_tidspunkt     TIMESTAMP WITHOUT TIME ZONE DEFAULT timezone('Europe/Oslo'::text, current_timestamp)
);

CREATE INDEX IF NOT EXISTS idx_outbox_pending ON outbox (id) WHERE status = 'PENDING';

CREATE OR REPLACE TRIGGER oppdater_endret_tidspunkt
    BEFORE UPDATE
    ON outbox
    FOR EACH ROW
EXECUTE FUNCTION oppdater_endret_tidspunkt();

DO
$$
BEGIN
        IF EXISTS
            (SELECT 1 FROM pg_roles WHERE rolname = 'cloudsqliamuser')
        THEN
            GRANT SELECT ON TABLE outbox TO cloudsqliamuser;
END IF;
END
$$;
