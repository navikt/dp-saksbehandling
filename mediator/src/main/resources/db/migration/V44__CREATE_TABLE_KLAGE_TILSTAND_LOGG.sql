CREATE TABLE IF NOT EXISTS klage_tilstand_logg_v1(
   id                      UUID        PRIMARY KEY,
   klage_id                UUID        NOT NULL REFERENCES klage_v1 (id) ON DELETE CASCADE,
   tilstand                TEXT        NOT NULL,
   hendelse_type           TEXT        NOT NULL,
   hendelse                JSONB       NOT NULL,
   tidspunkt               TIMESTAMP WITHOUT TIME ZONE NOT NULL
);
CREATE INDEX IF NOT EXISTS klage_tilstand_logg_klage_id_index ON klage_tilstand_logg_v1 (klage_id);
