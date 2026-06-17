DROP INDEX IF EXISTS oppgave_endret_tidspunkt_index;
CREATE INDEX IF NOT EXISTS oppgave_tilstand_logg_tidspunkt_index ON oppgave_tilstand_logg_v1 (tidspunkt);
