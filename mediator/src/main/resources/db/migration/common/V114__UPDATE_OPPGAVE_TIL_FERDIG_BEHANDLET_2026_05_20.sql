-- Pga timeout mot regelmotor har to oppgaver kommet ut av synk med regelmotor. En av oppgavene har fått sendt ut vedtaksbrev, den andre ikke.
DO $$
    DECLARE
        oppgave RECORD;
    BEGIN
        FOR oppgave IN
            SELECT  *
            FROM    oppgave_v1 oppg
            WHERE   oppg.tilstand = 'UNDER_BEHANDLING'
            AND     oppg.id IN ('019e0248-ce30-77fc-aba7-25bee05d5c06','019e0405-9e2e-74ac-aa6e-a8ef1b537ca5')
        LOOP
            UPDATE oppgave_v1
            SET    tilstand = 'FERDIG_BEHANDLET'
            WHERE  tilstand = 'UNDER_BEHANDLING'
            AND    id       = oppgave.id;

            IF oppgave.id = '019e0248-ce30-77fc-aba7-25bee05d5c06' THEN

                INSERT INTO oppgave_tilstand_logg_v1
                ( id
                , oppgave_id
                , tilstand
                , hendelse_type
                , hendelse
                , tidspunkt
                )
                VALUES
                    ( '019e449d-0e60-765f-a3df-06a135579c5e'
                    , '019e0248-ce30-77fc-aba7-25bee05d5c06'
                    , 'FERDIG_BEHANDLET'
                    , 'GodkjentBehandlingHendelse'
                    , '{"oppgaveId": "019e0248-ce30-77fc-aba7-25bee05d5c06", "utførtAv": {"grupper": ["2e9c63d8-322e-4c1f-b500-a0abb812761c"], "navIdent": "H157985", "tilganger": ["SAKSBEHANDLER"]}, "meldingOmVedtakKilde": "DP_SAK"}'
                    , now() + INTERVAL '2 hours'
                    );
            END IF;

            IF oppgave.id = '019e0405-9e2e-74ac-aa6e-a8ef1b537ca5' THEN
                INSERT INTO oppgave_tilstand_logg_v1
                ( id
                , oppgave_id
                , tilstand
                , hendelse_type
                , hendelse
                , tidspunkt
                )
                VALUES
                    ( '019e44a4-7b3c-71c8-941a-1a11545b2411'
                    , '019e0405-9e2e-74ac-aa6e-a8ef1b537ca5'
                    , 'FERDIG_BEHANDLET'
                    , 'GodkjentBehandlingHendelse'
                    , '{"oppgaveId": "019e0405-9e2e-74ac-aa6e-a8ef1b537ca5", "utførtAv": {"grupper": ["2e9c63d8-322e-4c1f-b500-a0abb812761c"], "navIdent": "S142108", "tilganger": ["SAKSBEHANDLER"]}, "meldingOmVedtakKilde": "DP_SAK"}'
                    , now() + INTERVAL '2 hours'
                    );

                INSERT INTO utsending_v1(
                    id,
                    behandling_id,
                    tilstand,
                    type
                )
                VALUES(
                  '019e44a6-e837-71aa-923d-cf47ffaffa99',
                  '019e0405-979e-7bc5-be00-32fe2dc5dd62',
                  'VenterPåVedtak',
                  'VEDTAK_DAGPENGER'
              );
            END IF;
        END LOOP;
    END $$;
