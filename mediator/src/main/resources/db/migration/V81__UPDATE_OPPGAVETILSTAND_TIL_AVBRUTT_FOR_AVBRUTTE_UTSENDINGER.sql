DROP TRIGGER IF EXISTS oppdater_endret_tidspunkt ON oppgave_v1;

DO $$
    DECLARE
        oppgave RECORD;
    BEGIN
        FOR oppgave IN
            SELECT  opp.id as oppgave_id
            FROM    utsending_v1    uts
            JOIN    behandling_v1   beh ON beh.id = uts.behandling_id
            JOIN    oppgave_v1      opp ON beh.id = opp.behandling_id
            WHERE   uts.tilstand    = 'Avbrutt'
            AND     opp.tilstand    = 'FERDIG_BEHANDLET'
        LOOP
            UPDATE  oppgave_v1
            SET     tilstand    = 'AVBRUTT'
            WHERE   id          = oppgave.oppgave_id
            AND     tilstand    = 'FERDIG_BEHANDLET'
            ;
            UPDATE  oppgave_tilstand_logg_v1
            SET     tilstand    = 'AVBRUTT'
            WHERE   oppgave_id  = oppgave.oppgave_id
            AND     tilstand    = 'FERDIG_BEHANDLET'
            ;
        END LOOP;
    END $$;

CREATE TRIGGER oppdater_endret_tidspunkt
    BEFORE UPDATE
    ON oppgave_v1
    FOR EACH ROW
EXECUTE FUNCTION oppdater_endret_tidspunkt();
