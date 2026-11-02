-- Pga. strikk-og-binders-løsningen for medhold, er det ikke mulig å endre utfall etter at man har satt medhold.
-- Når klagens tilstand har gått til BEHANDLING_UTFORT, kan man ikke ta den tilbake til BEHANDLES.
-- Saksbehandler ønsker å avbryte oppgaven, men det er ikke tillatt i tilstand BEHANDLING_UTFORT.
-- Derfor må vi endre tilstanden tilbake til BEHANDLES, og slette logginnslaget som sier at den er utført.
-- https://nav-it.slack.com/lists/T5LNAMWNA/F0BETLJKLAF?record_id=Rec0CD8AQLU92
UPDATE  klage_v1
SET     tilstand    = 'BEHANDLES'
WHERE   id          = '01a181db-540c-7620-a008-7656db899733'
AND     tilstand    = 'BEHANDLING_UTFORT'
;
DELETE
FROM    klage_tilstand_logg_v1
WHERE   klage_id    = '01a181db-540c-7620-a008-7656db899733'
AND     id          = '01a18669-cfa5-77bc-a2c5-3583d645ebb8'
AND     tilstand    = 'BEHANDLING_UTFORT'
;
