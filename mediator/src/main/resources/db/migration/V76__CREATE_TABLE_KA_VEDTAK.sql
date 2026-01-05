CREATE TABLE IF NOT EXISTS ka_vedtak_v1
(
    id                      UUID                        PRIMARY KEY,
    klage_id                UUID                        NOT NULL REFERENCES klage_v1 (id),
    utfall                  TEXT                        NOT NULL,
    avsluttet               TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    journalpost_ider        TEXT[]                      NOT NULL,
    registrert_tidspunkt    TIMESTAMP WITHOUT TIME ZONE DEFAULT timezone('Europe/Oslo'::text, current_timestamp)
)
