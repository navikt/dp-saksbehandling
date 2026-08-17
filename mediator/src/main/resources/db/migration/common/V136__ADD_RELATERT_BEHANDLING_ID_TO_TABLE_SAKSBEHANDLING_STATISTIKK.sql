ALTER TABLE saksbehandling_statistikk_v1
ADD COLUMN relatert_behandling_id UUID REFERENCES behandling_v1 (id) ON DELETE CASCADE
;
