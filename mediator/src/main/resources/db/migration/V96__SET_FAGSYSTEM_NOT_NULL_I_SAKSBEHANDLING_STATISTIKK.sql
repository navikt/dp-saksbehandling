TRUNCATE TABLE saksbehandling_statistikk_v1 RESTART IDENTITY
;
ALTER TABLE saksbehandling_statistikk_v1
ALTER COLUMN fagsystem SET NOT NULL
;