-- Tabell for generell oppgave-data (konvolutt + innhold)
CREATE TABLE IF NOT EXISTS generell_oppgave_data_v1
(
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    oppgave_id       UUID        NOT NULL REFERENCES oppgave_v1 (id) ON DELETE CASCADE UNIQUE,
    oppgave_type     TEXT        NOT NULL,
    tittel           TEXT        NOT NULL,
    beskrivelse      TEXT,
    strukturert_data JSONB,
    opprettet        TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_generell_oppgave_data_oppgave_id ON generell_oppgave_data_v1 (oppgave_id);
