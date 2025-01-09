-- Feil i dp-behandling medfører at alle innvilgelser før 6. januar 2025 kl 10:00 må behandles i Arena.
DO $$
DECLARE
    oppgave RECORD;
BEGIN
    FOR oppgave IN
        SELECT  *
        FROM    oppgave_v1 oppg
        WHERE   oppg.tilstand = 'KLAR_TIL_BEHANDLING'
        AND     oppg.opprettet < to_date('06-01-2025','dd-mm-yyyy')
        AND EXISTS (
                SELECT 1
                FROM   emneknagg_v1 emne
                WHERE  emne.oppgave_id = oppg.id
                AND    emne.emneknagg = 'Innvilgelse'
        )
    LOOP
            -- Oppdater oppgave_v1
            UPDATE  oppgave_v1 oppg
            SET     tilstand = 'BEHANDLES_I_ARENA'
            WHERE   oppg.id = oppgave.id;

            -- Sett inn logg i oppgave_tilstand_logg_v1
            INSERT INTO oppgave_tilstand_logg_v1
                ( id
                , oppgave_id
                , tilstand
                , hendelse_type
                , hendelse
                , tidspunkt
                )
            VALUES
                ( gen_random_uuid()
                , oppgave.id
                , 'BEHANDLES_I_ARENA'
                , 'SkriptHendelse'
                , '{"utførtAv": "V22__OPPGAVE_UPDATE_BEHANDLES_I_ARENA_2025_01_09"}'
                , now() + INTERVAL '1 hours'
                );
    END LOOP;
END $$;
