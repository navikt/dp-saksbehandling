-- Avbryt oppgave og utsending for person som har endret fra dnr til fnr.
-- Det er ikke mulig å fatte vedtak i Arena på gammelt dnr, som er registrert i dp-sak.
UPDATE  utsending_v1
SET     tilstand = 'Avbrutt'
WHERE   id       = '019d75f6-1696-767a-b9aa-539c8f3823ec'
AND     tilstand = 'VenterPåVedtak'
;
UPDATE  oppgave_tilstand_logg_v1
SET     tilstand      = 'AVBRUTT'
      , hendelse_type = 'SkriptHendelse'
      , hendelse      = '{"utførtAv": {"navn": "V108__AVBRYT_OPPGAVE_OG_UTSENDING_2026_04_10.sql"}}'
WHERE   oppgave_id    = '019d4a5f-fa11-77af-9769-2acaff38b437'
AND     tilstand      = 'FERDIG_BEHANDLET'
;
DROP TRIGGER IF EXISTS oppdater_endret_tidspunkt ON oppgave_v1
;
UPDATE  oppgave_v1
SET     tilstand    = 'AVBRUTT'
WHERE   id          = '019d4a5f-fa11-77af-9769-2acaff38b437'
AND     tilstand    = 'FERDIG_BEHANDLET'
;
CREATE TRIGGER oppdater_endret_tidspunkt
    BEFORE UPDATE
    ON oppgave_v1
    FOR EACH ROW
EXECUTE FUNCTION oppdater_endret_tidspunkt()
;
