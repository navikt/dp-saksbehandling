TRUNCATE TABLE saksbehandling_statistikk_v1 RESTART IDENTITY
;
ALTER TABLE saksbehandling_statistikk_v1
    ADD COLUMN resultat_begrunnelse TEXT
;