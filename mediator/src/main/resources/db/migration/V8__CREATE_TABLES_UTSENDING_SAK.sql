CREATE TABLE IF NOT EXISTS sak_v1
(
    id                   TEXT NOT NULL PRIMARY KEY,
    kontekst             TEXT NOT NULL,
    registrert_tidspunkt TIMESTAMP WITHOUT TIME ZONE DEFAULT timezone('Europe/Oslo'::text, current_timestamp),
    endret_tidspunkt     TIMESTAMP WITHOUT TIME ZONE DEFAULT timezone('Europe/Oslo'::text, current_timestamp)
);
CREATE TRIGGER oppdater_endret_tidspunkt
    BEFORE UPDATE
    ON sak_v1
    FOR EACH ROW
EXECUTE FUNCTION oppdater_endret_tidspunkt();

CREATE TABLE IF NOT EXISTS utsending_v1
(
    id                   UUID PRIMARY KEY,
    oppgave_id           UUID REFERENCES oppgave_v1 (id) ON DELETE CASCADE NOT NULL,
    tilstand             TEXT                                              NOT NULL,
    brev                 TEXT,
    pdf_urn              TEXT,
    journalpost_id       TEXT,
    distribusjon_id      TEXT,
    sak_id               TEXT references sak_v1 (id),
    registrert_tidspunkt TIMESTAMP WITHOUT TIME ZONE DEFAULT timezone('Europe/Oslo'::text, current_timestamp),
    endret_tidspunkt     TIMESTAMP WITHOUT TIME ZONE DEFAULT timezone('Europe/Oslo'::text, current_timestamp)
);

CREATE INDEX IF NOT EXISTS utsending_oppgave_id_index ON utsending_v1 (oppgave_id);
CREATE INDEX IF NOT EXISTS utsending_sak_id_index ON utsending_v1 (sak_id);
CREATE TRIGGER oppdater_endret_tidspunkt
    BEFORE UPDATE
    ON utsending_v1
    FOR EACH ROW
EXECUTE FUNCTION oppdater_endret_tidspunkt();



