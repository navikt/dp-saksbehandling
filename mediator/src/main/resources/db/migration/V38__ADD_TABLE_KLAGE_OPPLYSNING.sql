CREATE TABLE IF NOT EXISTS klage_v1
(
    id                    UUID PRIMARY KEY,
    tilstand              TEXT                        NOT NULL,
    registrert_tidspunkt  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT timezone('Europe/Oslo'::text, current_timestamp),
    endret_tidspunkt      TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT timezone('Europe/Oslo'::text, current_timestamp)
);

CREATE TABLE IF NOT EXISTS klage_opplysning_v1
(
    id                      UUID PRIMARY KEY,
    klage_id                UUID NOT NULL REFERENCES klage_v1(id) ON DELETE CASCADE,
    type                    TEXT NOT NULL,
--     verdi                   TEXT NOT NULL,
    registrert_tidspunkt    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT timezone('Europe/Oslo'::text, current_timestamp),
    endret_tidspunkt        TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT timezone('Europe/Oslo'::text, current_timestamp)
);

CREATE INDEX IF NOT EXISTS klage_opplysning_klage_index ON klage_opplysning_v1 (klage_id);

CREATE TRIGGER oppdater_endret_tidspunkt
BEFORE UPDATE ON klage_v1
FOR EACH ROW EXECUTE FUNCTION oppdater_endret_tidspunkt();
