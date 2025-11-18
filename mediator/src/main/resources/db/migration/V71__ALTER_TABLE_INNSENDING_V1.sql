ALTER TABLE innsending_v1
    DROP COLUMN IF EXISTS tilstand;

ALTER TABLE innsending_v1
    DROP COLUMN IF EXISTS behandler_ident;

ALTER TABLE innsending_v1
    ADD COLUMN IF NOT EXISTS soknad_id UUID NULL DEFAULT NULL;
