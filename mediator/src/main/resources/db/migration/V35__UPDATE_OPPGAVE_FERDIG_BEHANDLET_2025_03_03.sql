-- Pga. at Skattetetaten hadde problemer, ble det forsinkelse på innhenting av inntektsopplysninger.
-- Dette medførte at vedtaksforslag ble sendt ut før automatisk vedtak ble fattet.
-- Oppgave som ble opprettet som følge av vedtaksforslaget, ble tatt til behandling av saksbehandler før automatisk vedtak ble fattet.
-- Vi tillater ikke tilstandsendring på bakgrunn av VedtakFattetHendelse i tilstand UNDER_BEHANDLING
DO $$
    DECLARE
        oppgave RECORD;
    BEGIN
        FOR oppgave IN
            SELECT  *
            FROM    oppgave_v1 oppg
            WHERE   oppg.tilstand = 'UNDER_BEHANDLING'
            AND     oppg.id = '0195442d-22d4-78a6-95f4-154ea70e8858'
            LOOP
                -- Oppdater oppgave_v1
                UPDATE  oppgave_v1 oppg
                SET     tilstand = 'FERDIG_BEHANDLET',
                        saksbehandler_ident = null
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
                    , 'SkriptHendelse'
                    , '{"utførtAv": "V35__UPDATE_OPPGAVE_FERDIG_BEHANDLET_2025_03_03.sql"}'
                    , now() + INTERVAL '1 hours'
                    );
            END LOOP;
    END $$;
