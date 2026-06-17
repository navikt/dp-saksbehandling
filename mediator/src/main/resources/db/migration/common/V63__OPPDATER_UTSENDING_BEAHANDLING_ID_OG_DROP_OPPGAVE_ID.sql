DO $$
    DECLARE
        utsending RECORD;
    BEGIN
        ALTER TABLE utsending_v1 DISABLE TRIGGER oppdater_endret_tidspunkt;

        FOR utsending IN
            SELECT  oppg.behandling_id
                  , oppg.id AS oppgave_id
            FROM    oppgave_v1   oppg
            JOIN    utsending_v1 utse ON utse.oppgave_id = oppg.id
        LOOP
            UPDATE  utsending_v1
            SET     behandling_id = utsending.behandling_id
            WHERE   oppgave_id    = utsending.oppgave_id;
        END LOOP;

        ALTER TABLE utsending_v1 ENABLE TRIGGER oppdater_endret_tidspunkt;
        ALTER TABLE utsending_v1 DROP CONSTRAINT IF EXISTS oppgave_id_unique;
        ALTER TABLE utsending_v1 DROP COLUMN IF EXISTS oppgave_id;
        ALTER TABLE utsending_v1 ADD CONSTRAINT behandling_id_unique UNIQUE (behandling_id);
    END $$;
