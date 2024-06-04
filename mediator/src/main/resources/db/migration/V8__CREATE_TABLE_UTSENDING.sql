CREATE TABLE IF NOT EXISTS utsending_v1
(
    id UUID PRIMARY KEY,
    oppgave_id UUID REFERENCES oppgave_v1(id) ON DELETE CASCADE NOT NULL,
    tilstand TEXT NOT NULL,
    brev TEXT,
    pdf_urn TEXT,
    journalpost_id TEXT,
    registrert_tidspunkt TIMESTAMP WITHOUT TIME ZONE DEFAULT timezone('Europe/Oslo'::text, current_timestamp),
    endret_tidspunkt     TIMESTAMP WITHOUT TIME ZONE DEFAULT timezone('Europe/Oslo'::text, current_timestamp)
);

CREATE INDEX IF NOT EXISTS utsending_oppgave_id_index ON utsending_v1 (oppgave_id);

CREATE TRIGGER oppdater_endret_tidspunkt
    BEFORE UPDATE
    ON utsending_v1
    FOR EACH ROW
EXECUTE FUNCTION oppdater_endret_tidspunkt();