CREATE TABLE IF NOT EXISTS person_v1
(
    ident VARCHAR(11) PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS oppgave_v1
(
    id            UUID PRIMARY KEY,
    person_ident  VARCHAR(11) REFERENCES person_v1 (ident),
    opprettet     TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    behandling_id UUID                        NOT NULL,
    tilstand      TEXT                        NOT NULL
);

CREATE INDEX IF NOT EXISTS oppgave_person_ident_index ON oppgave_v1 (person_ident);

CREATE TABLE IF NOT EXISTS emneknagg_v1
(
    id         BIGSERIAL PRIMARY KEY,
    oppgave_id UUID REFERENCES oppgave_v1 (id),
    emneknagg  TEXT
);
CREATE INDEX IF NOT EXISTS emneknagg_oppgave_id_index ON emneknagg_v1 (oppgave_id);
