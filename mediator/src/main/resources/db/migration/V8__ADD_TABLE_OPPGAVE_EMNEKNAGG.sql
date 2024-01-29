CREATE TABLE IF NOT EXISTS oppgave_emneknagg
(
    id                   BIGSERIAL PRIMARY KEY,
    oppgave_uuid         UUID      NOT NULL REFERENCES oppgave (uuid),
    emneknagg            TEXT      NOT NULL,
    UNIQUE (oppgave_uuid, emneknagg)
);

CREATE INDEX IF NOT EXISTS oppgave_emneknagg_idx ON oppgave_emneknagg (oppgave_uuid);
