-- Oppdaterer tidspunkt pga feil i skript V99__UPDATE_OPPGAVE_FRA_AVBRUTT_TIL_FERDIG_BEHANDLET.sql.
UPDATE  oppgave_tilstand_logg_v1
SET     tidspunkt  = '2026-02-20 13:26:22'::timestamp
WHERE   id         = '019c897a-d113-71cc-ad77-3d3a0ffa6e15'
AND     oppgave_id = '019c67c0-83bb-7442-a638-6a74c235cfd6'
;

UPDATE saksbehandling_statistikk_v1
SET     tilstand_tidspunkt = '2026-02-20 13:26:22'::timestamp
WHERE   sekvensnummer = 111462
AND     tilstand_id   = '019c897a-d113-71cc-ad77-3d3a0ffa6e15'
;
