CREATE TABLE IF NOT EXISTS saksbehandling_statistikk_v1
(
    oppgave_id              UUID PRIMARY KEY,
    mottatt                 TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    sak_id                  UUID                        NOT NULL,
    behandling_id           UUID                        NOT NULL,
    person_ident            TEXT                        NOT NULL,
    saksbehandler_ident     TEXT,
    beslutter_ident         TEXT,
    versjon                 TEXT                        NOT NULL,
    tilstand                TEXT                        NOT NULL,
    tilstand_id             UUID                        NOT NULL,
    tilstand_tidspunkt      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    utlost_av               TEXT                        NOT NULL,
    overfort_til_statistikk BOOLEAN                     NOT NULL DEFAULT false
);

DO
$$
    BEGIN
        IF EXISTS
                (SELECT 1 FROM pg_roles WHERE rolname = 'cloudsqliamuser')
        THEN
            GRANT SELECT ON TABLE saksbehandling_statistikk_v1 TO cloudsqliamuser;
        END IF;
    END
$$;
