CREATE TABLE IF NOT EXISTS sak_v2
(
    id                   UUID PRIMARY KEY,
    person_id            UUID                     NOT NULL REFERENCES person_v1 (id),
    soknad_id            UUID                     NOT NULL,
    opprettet            TIMESTAMP WITH TIME ZONE NOT NULL,
    registrert_tidspunkt TIMESTAMP WITHOUT TIME ZONE DEFAULT timezone('Europe/Oslo'::text, current_timestamp),
    endret_tidspunkt     TIMESTAMP WITHOUT TIME ZONE DEFAULT timezone('Europe/Oslo'::text, current_timestamp)

);
CREATE INDEX IF NOT EXISTS sak_person_id_index ON sak_v2 (person_id);
CREATE INDEX IF NOT EXISTS sak_soknad_id_index ON sak_v2 (soknad_id);
CREATE TRIGGER oppdater_endret_tidspunkt
    BEFORE UPDATE
    ON sak_v2
    FOR EACH ROW
EXECUTE FUNCTION oppdater_endret_tidspunkt();
