CREATE TABLE IF NOT EXISTS henvendelse_v1
(
    id                   UUID PRIMARY KEY,
    person_id            UUID                        NOT NULL REFERENCES person_v1 (id),
    journalpost_id       TEXT                        NOT NULL,
    mottatt              TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    skjema_kode          TEXT                        NOT NULL,
    kategori             TEXT                        NOT NULL,
    tilstand             TEXT                        NOT NULL,
    behandler_ident      TEXT,
    registrert_tidspunkt TIMESTAMP WITHOUT TIME ZONE DEFAULT timezone('Europe/Oslo'::text, current_timestamp),
    endret_tidspunkt     TIMESTAMP WITHOUT TIME ZONE DEFAULT timezone('Europe/Oslo'::text, current_timestamp)

);
CREATE INDEX IF NOT EXISTS henvendelse_person_id_index ON henvendelse_v1 (person_id);
CREATE TRIGGER oppdater_endret_tidspunkt
    BEFORE UPDATE
    ON henvendelse_v1
    FOR EACH ROW
EXECUTE FUNCTION oppdater_endret_tidspunkt();

CREATE TABLE IF NOT EXISTS henvendelse_tilstand_logg_v1(
    id                      UUID        PRIMARY KEY,
    henvendelse_id          UUID        NOT NULL REFERENCES henvendelse_v1 (id) ON DELETE CASCADE,
    tilstand                TEXT        NOT NULL,
    hendelse_type           TEXT        NOT NULL,
    hendelse                JSONB       NOT NULL,
    tidspunkt               TIMESTAMP WITHOUT TIME ZONE NOT NULL
);
CREATE INDEX IF NOT EXISTS henvendelse_tilstand_logg_henvendelse_id_index ON henvendelse_tilstand_logg_v1 (henvendelse_id);