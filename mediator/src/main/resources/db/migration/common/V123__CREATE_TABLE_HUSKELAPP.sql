CREATE TABLE IF NOT EXISTS huskelapp_v1
(
    oppgave_id              UUID PRIMARY KEY REFERENCES oppgave_v1 (id),
    tekst                   TEXT,
    skrevet_av              TEXT,
    registrert_tidspunkt    TIMESTAMP WITHOUT TIME ZONE DEFAULT timezone('Europe/Oslo'::text, current_timestamp),
    endret_tidspunkt        TIMESTAMP WITHOUT TIME ZONE DEFAULT timezone('Europe/Oslo'::text, current_timestamp)
);