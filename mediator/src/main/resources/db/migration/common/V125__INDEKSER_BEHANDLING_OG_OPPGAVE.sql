CREATE INDEX IF NOT EXISTS behandling_utlost_av_index ON behandling_v1 (utlost_av);
CREATE INDEX IF NOT EXISTS oppgave_tilstand_opprettet_index ON oppgave_v1 (tilstand, opprettet);