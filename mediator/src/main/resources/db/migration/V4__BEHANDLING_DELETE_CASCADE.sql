ALTER TABLE oppgave_v1
    DROP CONSTRAINT IF EXISTS oppgave_v1_behandling_id_fkey;
ALTER TABLE oppgave_v1
    ADD CONSTRAINT oppgave_v1_behandling_id_fkey FOREIGN KEY (behandling_id) REFERENCES behandling_v1 (id) ON DELETE CASCADE;

ALTER TABLE emneknagg_v1
    DROP CONSTRAINT IF EXISTS emneknagg_v1_oppgave_id_fkey;
ALTER TABLE emneknagg_v1
    ADD CONSTRAINT emneknagg_v1_oppgave_id_fkey FOREIGN KEY (oppgave_id) REFERENCES oppgave_v1 (id) ON DELETE CASCADE;

