CREATE TABLE IF NOT EXISTS oppgave_tilstand_logg_v1(
    id                      UUID        PRIMARY KEY,
    oppgave_id              UUID        NOT NULL REFERENCES oppgave_v1 (id) ON DELETE CASCADE,
    tilstand                TEXT        NOT NULL,
    hendelse                JSONB       NOT NULL,
    registrert_tidspunkt    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS oppgave_tilstand_logg_oppgave_id_index ON oppgave_tilstand_logg_v1 (oppgave_id);