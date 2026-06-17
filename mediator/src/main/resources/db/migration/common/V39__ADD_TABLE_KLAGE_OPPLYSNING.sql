CREATE TABLE IF NOT EXISTS klage_v1
(
    id                    UUID PRIMARY KEY,
    tilstand              TEXT                        NOT NULL,
    opplysninger         JSONB                       NOT NULL,
    registrert_tidspunkt  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT timezone('Europe/Oslo'::text, current_timestamp),
    endret_tidspunkt      TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT timezone('Europe/Oslo'::text, current_timestamp)

);

CREATE OR REPLACE TRIGGER oppdater_endret_tidspunkt
BEFORE UPDATE ON klage_v1
FOR EACH ROW EXECUTE FUNCTION oppdater_endret_tidspunkt();
