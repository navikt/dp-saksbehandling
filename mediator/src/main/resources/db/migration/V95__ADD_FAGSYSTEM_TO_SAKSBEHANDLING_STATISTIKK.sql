TRUNCATE TABLE saksbehandling_statistikk_v1 RESTART IDENTITY
;
ALTER TABLE saksbehandling_statistikk_v1
ADD COLUMN fagsystem TEXT
;
ALTER TABLE saksbehandling_statistikk_v1
    ADD COLUMN behandling_aarsak TEXT
;
ALTER TABLE saksbehandling_statistikk_v1
    ADD COLUMN arena_sak_id TEXT
;
