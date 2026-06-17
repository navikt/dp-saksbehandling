CREATE TABLE IF NOT EXISTS hendelse_v1
(
    behandling_id         UUID PRIMARY KEY REFERENCES behandling_v1(id) ON DELETE CASCADE NOT NULL,
    hendelse_type         TEXT NOT NULL,
    hendelse_data         JSONB NOT NULL
);

