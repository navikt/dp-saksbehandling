TRUNCATE TABLE saksbehandling_statistikk_v1;

ALTER TABLE saksbehandling_statistikk_v1 DROP CONSTRAINT saksbehandling_statistikk_v1_pkey;

ALTER TABLE saksbehandling_statistikk_v1 ADD COLUMN sekvensnummer BIGSERIAL PRIMARY KEY;
