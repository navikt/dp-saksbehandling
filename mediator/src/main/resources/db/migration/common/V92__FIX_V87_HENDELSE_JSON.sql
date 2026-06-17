-- V87 wrote utførtAv as a plain string instead of a JSON object with a "navn" field.
-- This script fixes the hendelse JSON for rows affected by V87.
UPDATE  oppgave_tilstand_logg_v1
SET     hendelse = '{"utførtAv": {"navn": "V87__UPDATE_HENDELSE_TYPE_TIL_SKRIPT_HENDELSE.sql"}}'
WHERE   hendelse_type = 'SkriptHendelse'
AND     hendelse = '{"utførtAv": "V87__UPDATE_HENDELSE_TYPE_TIL_SKRIPT_HENDELSE.sql"}'
;
