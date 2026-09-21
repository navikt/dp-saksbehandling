CREATE TABLE IF NOT EXISTS tilbakekreving_v1
(
    id                         UUID PRIMARY KEY,
    opprettet                  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    avvent_behandling_til_dato DATE,
    varsel_sendt               DATE,
    behandlingsstatus          TEXT                        NOT NULL,
    forrige_behandlingsstatus  TEXT,
    totalt_feilutbetalt_belop  NUMERIC,
    saksbehandling_url         TEXT                        NOT NULL,
    fullstendig_periode_fom    DATE                        NOT NULL,
    fullstendig_periode_tom    DATE                        NOT NULL,
    registrert_tidspunkt       TIMESTAMP WITHOUT TIME ZONE DEFAULT timezone('Europe/Oslo'::text, current_timestamp),
    endret_tidspunkt           TIMESTAMP WITHOUT TIME ZONE DEFAULT timezone('Europe/Oslo'::text, current_timestamp)
);

CREATE OR REPLACE TRIGGER oppdater_endret_tidspunkt
    BEFORE UPDATE
    ON tilbakekreving_v1
    FOR EACH ROW
EXECUTE FUNCTION oppdater_endret_tidspunkt();

