ALTER TABLE oppgave_v1 DISABLE TRIGGER oppdater_endret_tidspunkt;

UPDATE oppgave_v1
SET    tilstand = 'AVBRUTT'
WHERE  tilstand = 'BEHANDLES_I_ARENA';

UPDATE oppgave_tilstand_logg_v1
SET    tilstand = 'AVBRUTT'
WHERE  tilstand = 'BEHANDLES_I_ARENA';

ALTER TABLE oppgave_v1 ENABLE TRIGGER oppdater_endret_tidspunkt;
