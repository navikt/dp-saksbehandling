ALTER TABLE IF EXISTS saksbehandling_statistikk_v1
ADD COLUMN IF NOT EXISTS emneknagger TEXT[]
;
