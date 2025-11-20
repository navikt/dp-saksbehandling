UPDATE  innsending_v1
SET     tilstand = 'BEHANDLES';

ALTER TABLE innsending_v1
    DROP COLUMN IF EXISTS behandler_ident;

ALTER TABLE innsending_v1
    ADD COLUMN IF NOT EXISTS soknad_id UUID DEFAULT NULL;

ALTER TABLE innsending_v1
    ALTER COLUMN tilstand SET DEFAULT 'BEHANDLES';

ALTER TABLE innsending_v1
    ADD COLUMN IF NOT EXISTS vurdering TEXT DEFAULT NULL;

ALTER TABLE innsending_v1
    ADD COLUMN IF NOT EXISTS resultat_behandling_id UUID DEFAULT NULL;

ALTER TABLE innsending_v1
    ADD COLUMN IF NOT EXISTS resultat_type TEXT DEFAULT NULL;
