ALTER TABLE utsending_v1
ADD COLUMN IF NOT EXISTS behandling_id UUID REFERENCES behandling_v1 (id);

CREATE INDEX IF NOT EXISTS utsending_behandling_id_index ON utsending_v1 (behandling_id);
