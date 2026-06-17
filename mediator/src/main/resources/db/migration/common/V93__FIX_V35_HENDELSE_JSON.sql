-- V35 wrote utførtAv as a plain string instead of a JSON object with a "navn" field.
UPDATE  oppgave_tilstand_logg_v1
SET     hendelse = '{"utførtAv": {"navn": "V35__UPDATE_OPPGAVE_FERDIG_BEHANDLET_2025_03_03.sql"}}'
WHERE   hendelse_type = 'SkriptHendelse'
AND     hendelse = '{"utførtAv": "V35__UPDATE_OPPGAVE_FERDIG_BEHANDLET_2025_03_03.sql"}'
;
