ALTER TABLE IF EXISTS oppgave_v1
    ADD COLUMN IF NOT EXISTS melding_om_vedtak_kilde TEXT NOT NULL DEFAULT 'DP_SAK';

ALTER TABLE IF EXISTS oppgave_v1
    ALTER COLUMN melding_om_vedtak_kilde DROP DEFAULT;
