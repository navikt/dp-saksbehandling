CREATE TABLE IF NOT EXISTS inhabilitet_v1
(
    person_id               UUID        NOT NULL REFERENCES person_v1 (id),
    nav_ident               VARCHAR(20) NOT NULL,
    registrert_tidspunkt    TIMESTAMP WITHOUT TIME ZONE DEFAULT timezone('Europe/Oslo'::text, current_timestamp),
    PRIMARY KEY (person_id, nav_ident)
);

