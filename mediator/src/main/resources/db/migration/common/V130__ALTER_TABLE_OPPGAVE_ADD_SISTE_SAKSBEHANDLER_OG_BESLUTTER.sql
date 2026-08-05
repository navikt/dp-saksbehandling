ALTER TABLE oppgave_v1 DISABLE TRIGGER oppdater_endret_tidspunkt
;
ALTER TABLE oppgave_v1
ADD COLUMN siste_saksbehandler_ident TEXT,
ADD COLUMN siste_beslutter_ident TEXT
;
ALTER TABLE oppgave_v1
RENAME COLUMN saksbehandler_ident TO behandler_ident
;
WITH oppgave AS
(
    SELECT    oppg.id
            , logg.hendelse -> 'utførtAv' ->> 'navIdent'::TEXT AS siste_saksbehandler_ident
    FROM    oppgave_v1 oppg
    JOIN    oppgave_tilstand_logg_v1 logg
        ON  logg.oppgave_id = oppg.id
        AND logg.id = ( SELECT  logg2.id
                        FROM    oppgave_tilstand_logg_v1 logg2
                        WHERE   logg2.oppgave_id = oppg.id
                        AND     logg2.tilstand IN('UNDER_BEHANDLING','PAA_VENT')
                        AND     logg2.hendelse_type IN ('NesteOppgaveHendelse', 'SettOppgaveAnsvarHendelse','OpprettOppfølgingHendelse')
                        AND     logg2.tidspunkt = ( SELECT  MAX(logg3.tidspunkt)
                                                    FROM    oppgave_tilstand_logg_v1 logg3
                                                    WHERE   logg3.oppgave_id = oppg.id
                                                    AND     logg3.tilstand IN('UNDER_BEHANDLING','PAA_VENT')
                                                    AND     logg3.hendelse_type IN ('SettOppgaveAnsvarHendelse', 'NesteOppgaveHendelse','OpprettOppfølgingHendelse')
                                                    AND     logg3.hendelse -> 'utførtAv' ->> 'navIdent'::TEXT IS NOT NULL
                                                  )
                      )
)
UPDATE  oppgave_v1 uopp
SET     siste_saksbehandler_ident = oppgave.siste_saksbehandler_ident
FROM    oppgave
WHERE   oppgave.id = uopp.id
;
WITH oppgave AS
(
    SELECT  oppg.id
          , logg.hendelse -> 'utførtAv' ->> 'navIdent'::TEXT AS siste_beslutter_ident
    FROM    oppgave_v1 oppg
    JOIN    oppgave_tilstand_logg_v1 logg
    ON      logg.oppgave_id = oppg.id
    AND     logg.id = ( SELECT  logg2.id
                        FROM    oppgave_tilstand_logg_v1 logg2
                        WHERE   logg2.oppgave_id = oppg.id
                        AND     logg2.tilstand = 'UNDER_KONTROLL'
                        AND     logg2.hendelse_type IN ('NesteOppgaveHendelse', 'SettOppgaveAnsvarHendelse')
                        AND     logg2.tidspunkt = ( SELECT  MAX(logg3.tidspunkt)
                                                    FROM    oppgave_tilstand_logg_v1 logg3
                                                    WHERE   logg3.oppgave_id = oppg.id
                                                    AND     logg3.tilstand = 'UNDER_KONTROLL'
                                                    AND     logg3.hendelse_type IN ('SettOppgaveAnsvarHendelse', 'NesteOppgaveHendelse')
                                                    AND     logg3.hendelse -> 'utførtAv' ->> 'navIdent'::TEXT IS NOT NULL
                                                  )
                      )
)
UPDATE  oppgave_v1 uopp
SET     siste_beslutter_ident = oppgave.siste_beslutter_ident
FROM    oppgave
WHERE   oppgave.id = uopp.id
;
ALTER TABLE oppgave_v1 ENABLE TRIGGER oppdater_endret_tidspunkt
;
