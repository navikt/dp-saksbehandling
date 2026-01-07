CREATE TABLE IF NOT EXISTS oppgave_til_statistikk_v1
(
    oppgave_id                 UUID PRIMARY KEY,
    ferdig_behandlet_tidspunkt TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    overfort_til_statistikk    BOOLEAN                     NOT NULL DEFAULT false
);

CREATE INDEX IF NOT EXISTS oppgave_til_statistikk_ferdig_behandlet_tidspunkt_index
    ON oppgave_til_statistikk_v1 (ferdig_behandlet_tidspunkt);

DO
$$
    BEGIN
        IF EXISTS
                (SELECT 1 FROM pg_roles WHERE rolname = 'cloudsqliamuser')
        THEN
            GRANT SELECT ON TABLE oppgave_til_statistikk_v1 TO cloudsqliamuser;
        END IF;
    END
$$;
