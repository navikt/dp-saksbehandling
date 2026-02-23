-- Oppdaterer oppgave og tilstand_logg til FERDIG_BEHANDLET. Pga en feil som følge av timeout mot regelmotor,
-- har saksbehandler etterpå avbrutt oppgaven for å få den ut av lista si.
-- I praksis er vedtak fattet og oppgave ferdig behandlet (vedtak i Arena).
UPDATE  oppgave_v1
SET     tilstand = 'FERDIG_BEHANDLET'
WHERE   tilstand = 'AVBRUTT'
AND     id = '019c67c0-83bb-7442-a638-6a74c235cfd6'
;
UPDATE  oppgave_tilstand_logg_v1
SET     tilstand = 'FERDIG_BEHANDLET'
    ,   hendelse_type = 'SkriptHendelse'
    ,   hendelse = '{"utførtAv": {"navn": "V99__UPDATE_OPPGAVE_FRA_AVBRUTT_TIL_FERDIG_BEHANDLET.sql"}}'
    ,   tidspunkt = '2026-02-20 09:53:26'::timestamp
WHERE   tilstand = 'AVBRUTT'
AND     oppgave_id = '019c67c0-83bb-7442-a638-6a74c235cfd6'
;
