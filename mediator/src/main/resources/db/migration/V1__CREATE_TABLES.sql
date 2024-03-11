CREATE TABLE IF NOT EXISTS person_v1
(
    id    UUID PRIMARY KEY,
    ident VARCHAR(11) NOT NULL UNIQUE
);
CREATE INDEX IF NOT EXISTS person_ident_index ON person_v1 (ident);

CREATE TABLE IF NOT EXISTS behandling_v1
(
    id        UUID PRIMARY KEY,
    person_id UUID                     NOT NULL REFERENCES person_v1 (id),
    opprettet TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX IF NOT EXISTS behandling_person_id_index ON behandling_v1 (person_id);

CREATE TABLE IF NOT EXISTS oppgave_v1
(
    id            UUID PRIMARY KEY,
    behandling_id UUID                     NOT NULL REFERENCES behandling_v1 (id),
    tilstand      TEXT                     NOT NULL,
    opprettet     TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX IF NOT EXISTS oppgave_behandling_id_index ON oppgave_v1 (behandling_id);

CREATE TABLE IF NOT EXISTS emneknagg_v1
(
    id         BIGSERIAL PRIMARY KEY,
    oppgave_id UUID NOT NULL REFERENCES oppgave_v1 (id),
    emneknagg  TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS emneknagg_oppgave_id_index ON emneknagg_v1 (oppgave_id);
