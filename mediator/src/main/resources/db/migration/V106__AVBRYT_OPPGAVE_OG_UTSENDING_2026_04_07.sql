-- Avbryt oppgave og utsending for person som har endret fra dnr til fnr.
-- Det er ikke mulig å fatte vedtak i Arena på gammelt dnr, som er registrert i dp-sak.
UPDATE  utsending_v1
SET     tilstand = 'Avbrutt'
WHERE   id       = '019d66ea-c5c9-73ec-a34f-b9dbbb7f0ed9'
AND     tilstand = 'VenterPåVedtak'
;
UPDATE  oppgave_tilstand_logg_v1
SET     tilstand    = 'AVBRUTT'
WHERE   oppgave_id  = '019d25ce-34db-7229-beb6-aab34ce6e6f1'
AND     tilstand    = 'FERDIG_BEHANDLET'
;
DROP TRIGGER IF EXISTS oppdater_endret_tidspunkt ON oppgave_v1
;
UPDATE  oppgave_v1
SET     tilstand    = 'AVBRUTT'
WHERE   id          = '019d25ce-34db-7229-beb6-aab34ce6e6f1'
AND     tilstand    = 'FERDIG_BEHANDLET'
;
CREATE TRIGGER oppdater_endret_tidspunkt
    BEFORE UPDATE
    ON oppgave_v1
    FOR EACH ROW
EXECUTE FUNCTION oppdater_endret_tidspunkt()
;


