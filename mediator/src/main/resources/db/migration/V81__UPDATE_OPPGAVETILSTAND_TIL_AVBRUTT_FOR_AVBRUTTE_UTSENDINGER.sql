SET session_replication_role = replica;

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
            AND     opp.id          = '019626bc-71f6-79f1-b826-698ece94e14e'
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

SET session_replication_role = origin;
