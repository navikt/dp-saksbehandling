-- Oppdaterer tidspunkt en time fram pga at det ble diff mellom tidspunktet på BigQuery (som har UTC) og dp-saksbehandling (som har Oslo/Europe)
UPDATE  oppgave_tilstand_logg_v1
SET     tidspunkt  = '2026-02-20 14:26:22'::timestamp
WHERE   id         = '019c897a-d113-71cc-ad77-3d3a0ffa6e15'
AND     oppgave_id = '019c67c0-83bb-7442-a638-6a74c235cfd6'
;

UPDATE  saksbehandling_statistikk_v1
SET     tilstand_tidspunkt = '2026-02-20 14:26:22'::timestamp
WHERE   sekvensnummer = 111462
AND     tilstand_id   = '019c897a-d113-71cc-ad77-3d3a0ffa6e15'
;
