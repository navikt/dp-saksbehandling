CREATE TABLE IF NOT EXISTS person_v1
(
    id    UUID PRIMARY KEY,
    ident VARCHAR(11) UNIQUE
);

CREATE TABLE IF NOT EXISTS behandling_v1
(
    id        UUID PRIMARY KEY,
    person_id UUID REFERENCES person_v1 (id),
    opprettet TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS oppgave_v1
(
    id            UUID PRIMARY KEY,
    behandling_id UUID REFERENCES behandling_v1 (id),
    tilstand      TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS emneknagg_v1
(
    id         BIGSERIAL PRIMARY KEY,
    oppgave_id UUID REFERENCES oppgave_v1 (id),
    emneknagg  TEXT
);
CREATE INDEX IF NOT EXISTS emneknagg_oppgave_id_index ON emneknagg_v1 (oppgave_id);
