ALTER TABLE oppgave_v1 ALTER COLUMN behandling_id  ON DELETE CASCADE;

-- ALTER TABLE emneknagg_v1
--     ALTER COLUMN registrert_tidspunkt TYPE TIMESTAMP WITH TIME ZONE USING registrert_tidspunkt AT TIME ZONE 'UTC';
-- ALTER TABLE emneknagg_v1
--     ALTER COLUMN endret_tidspunkt TYPE TIMESTAMP WITH TIME ZONE USING endret_tidspunkt AT TIME ZONE 'UTC';
