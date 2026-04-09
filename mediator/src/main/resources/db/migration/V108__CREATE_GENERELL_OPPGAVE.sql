-- Tabell for generelle oppgaver (selvstendig entitet, ikke koblet til oppgave_v1)
CREATE TABLE IF NOT EXISTS generell_oppgave_v1
(
    id                      UUID        PRIMARY KEY,
    person_id               UUID        NOT NULL REFERENCES person_v1 (id),
    tittel                  TEXT        NOT NULL,
    beskrivelse             TEXT,
    strukturert_data        JSONB,
    opprettet               TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    tilstand                TEXT        NOT NULL DEFAULT 'BEHANDLES',
    vurdering               TEXT,
    resultat_type           TEXT,
    resultat_behandling_id  UUID,
    valgt_sak_id            UUID,
    registrert_tidspunkt    TIMESTAMP WITHOUT TIME ZONE DEFAULT timezone('Europe/Oslo'::text, current_timestamp),
    endret_tidspunkt        TIMESTAMP WITHOUT TIME ZONE DEFAULT timezone('Europe/Oslo'::text, current_timestamp)
);

CREATE INDEX IF NOT EXISTS idx_generell_oppgave_person_id ON generell_oppgave_v1 (person_id);
CREATE INDEX IF NOT EXISTS idx_generell_oppgave_tilstand ON generell_oppgave_v1 (tilstand);

CREATE OR REPLACE TRIGGER oppdater_endret_tidspunkt
    BEFORE UPDATE
    ON generell_oppgave_v1
    FOR EACH ROW
EXECUTE FUNCTION oppdater_endret_tidspunkt();
