-- Pga. timeout mot regelmotor, oppstod inkonsistens, slik at oppgave ble stående i tilstand UNDER_BEHANDLING etter at vedtak var fattet.
-- Vi oppdaterer manuelt til FERDIG_BEHANDLET her.
DO $$
    DECLARE
        oppgave RECORD;
    BEGIN
        FOR oppgave IN
            SELECT  *
            FROM    oppgave_v1 oppg
            WHERE   oppg.tilstand = 'UNDER_BEHANDLING'
            AND     oppg.id = '019ad567-0e16-77c9-bea7-38446159c61a'
            LOOP
                -- Oppdater oppgave_v1
                UPDATE  oppgave_v1 oppg
                SET     tilstand = 'FERDIG_BEHANDLET'
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
                    , 'FERDIG_BEHANDLET'
                    , 'GodkjentBehandlingHendelse'
                    , '{"oppgaveId": "019ad567-0e16-77c9-bea7-38446159c61a", "utførtAv": {"grupper": ["2e9c63d8-322e-4c1f-b500-a0abb812761c"], "navIdent": "A164486", "tilganger": ["SAKSBEHANDLER"]}, "meldingOmVedtakKilde": "DP_SAK"}'
                    , now() + INTERVAL '1 hours'
                    );
            END LOOP;
    END $$;
