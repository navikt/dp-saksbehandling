CREATE TABLE IF NOT EXISTS notat_v1(
    id                          UUID        PRIMARY KEY,
    oppgave_tilstand_logg_id    UUID        UNIQUE NOT NULL REFERENCES oppgave_tilstand_logg_v1 (id) ON DELETE CASCADE,
    tekst                       TEXT        NOT NULL,
    endret_tidspunkt            TIMESTAMP WITHOUT TIME ZONE NOT NULL default CURRENT_TIMESTAMP
    );

CREATE INDEX IF NOT EXISTS notat_oppgave_tilstand_logg_id_index ON notat_v1 (oppgave_tilstand_logg_id);


CREATE TRIGGER oppdater_endret_tidspunkt
    BEFORE UPDATE ON notat_v1
    FOR EACH ROW EXECUTE FUNCTION oppdater_endret_tidspunkt();