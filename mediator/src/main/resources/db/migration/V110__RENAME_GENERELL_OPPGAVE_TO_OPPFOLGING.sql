ALTER TABLE generell_oppgave_v1 RENAME TO oppfolging_v1;
ALTER INDEX idx_generell_oppgave_person_id RENAME TO idx_oppfolging_person_id;
ALTER INDEX idx_generell_oppgave_tilstand RENAME TO idx_oppfolging_tilstand;
DROP TRIGGER IF EXISTS oppdater_endret_tidspunkt ON oppfolging_v1;
ALTER TABLE oppfolging_v1 RENAME CONSTRAINT generell_oppgave_v1_person_id_fkey TO oppfolging_v1_person_id_fkey;
CREATE OR REPLACE TRIGGER oppdater_endret_tidspunkt
    BEFORE UPDATE ON oppfolging_v1
    FOR EACH ROW EXECUTE FUNCTION oppdater_endret_tidspunkt();
