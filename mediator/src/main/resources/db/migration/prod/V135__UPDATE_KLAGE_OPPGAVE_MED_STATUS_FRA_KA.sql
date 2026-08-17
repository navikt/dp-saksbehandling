-- FØRSTE STADFESTELSE HOS KLAGEINSTANS
INSERT INTO emneknagg_v1
( oppgave_id
, emneknagg
)
SELECT '019b983f-046a-7415-856f-dd6e50699699'
     , 'Stadfestelse hos klageinstans'
FROM  oppgave_v1
WHERE id = '019b983f-046a-7415-856f-dd6e50699699'
;
INSERT INTO oppgave_tilstand_logg_v1
( id
, oppgave_id
, tilstand
, hendelse_type
, hendelse
, tidspunkt
)
SELECT '019fffa3-c9bd-779c-9b1e-0faa553273b7'
     , '019b983f-046a-7415-856f-dd6e50699699'
     , 'FERDIG_BEHANDLET'
     , 'KlageinstansVedtakHendelse'
     , '{"type": "KLAGE", "utfall": "STADFESTELSE", "klageId": "019b983f-039c-70ce-bcaa-8496dfc9b528", "avsluttet": "2026-01-13T14:51:02.464126", "utførtAv": { "navn": "Kabal" },"journalpostIder": ["736228617"], "klageinstansVedtakId": "af25af05-c138-4f20-8e9c-10243c966a0d" }'
     , now() + INTERVAL '2 hours'
FROM  oppgave_v1
WHERE id = '019b983f-046a-7415-856f-dd6e50699699'
;

-- ANDRE STADFESTELSE HOS KLAGEINSTANS
INSERT INTO emneknagg_v1(
  oppgave_id
, emneknagg
)
SELECT'019ef8d3-07ca-73ab-b88d-22713f9a7499'
    , 'Stadfestelse hos klageinstans'
FROM  oppgave_v1
WHERE id = '019ef8d3-07ca-73ab-b88d-22713f9a7499'
;
INSERT INTO oppgave_tilstand_logg_v1
( id
, oppgave_id
, tilstand
, hendelse_type
, hendelse
, tidspunkt
)
SELECT '019fffa4-ace3-70df-834b-86ee3269fd7b'
     , '019ef8d3-07ca-73ab-b88d-22713f9a7499'
     , 'FERDIG_BEHANDLET'
     , 'KlageinstansVedtakHendelse'
     , '{"type": "KLAGE", "utfall": "STADFESTELSE", "klageId": "019ef8d3-075a-74c5-8ba5-266656336173", "avsluttet": "2026-08-06T15:04:10.239521", "utførtAv": { "navn": "Kabal" },"journalpostIder": ["761542319"], "klageinstansVedtakId": "5d446b8a-c3d7-4ba0-b72f-6b0df18ae2d3" }'
     , now() + INTERVAL '2 hours'
FROM  oppgave_v1
WHERE id = '019ef8d3-07ca-73ab-b88d-22713f9a7499'
;

-- AVVIST
INSERT INTO emneknagg_v1(
  oppgave_id
, emneknagg
)
SELECT '019ef8c3-49ad-72a0-bfdd-701113b9c5fb'
     , 'Avvist'
FROM  oppgave_v1
WHERE id = '019ef8c3-49ad-72a0-bfdd-701113b9c5fb'
;

-- MEDHOLD
INSERT INTO emneknagg_v1(
  oppgave_id
, emneknagg
)
SELECT '019ef8bd-a956-74bd-aab4-6f4027625531'
     , 'Medhold'
FROM  oppgave_v1
WHERE id = '019ef8bd-a956-74bd-aab4-6f4027625531'
;

-- MEDHOLD
INSERT INTO emneknagg_v1(
  oppgave_id
, emneknagg
)
SELECT '019f364f-e1ea-76f9-898a-f7857289e0fe'
     , 'Medhold'
FROM  oppgave_v1
WHERE id = '019f364f-e1ea-76f9-898a-f7857289e0fe'
;
