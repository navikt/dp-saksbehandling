ALTER TABLE innsending_v1
    DROP COLUMN IF EXISTS tilstand;

ALTER TABLE innsending_v1
    DROP COLUMN IF EXISTS behandler_ident;

ALTER TABLE innsending_v1
    ADD COLUMN IF NOT EXISTS soknad_id UUID DEFAULT NULL;

ALTER TABLE innsending_v1
    ADD COLUMN IF NOT EXISTS vurdering TEXT DEFAULT NULL;

ALTER TABLE innsending_v1
    ADD COLUMN IF NOT EXISTS foerte_til_behandling_id UUID DEFAULT NULL;

ALTER TABLE innsending_v1
    ADD COLUMN IF NOT EXISTS foerte_til_behandling_type TEXT DEFAULT NULL;
