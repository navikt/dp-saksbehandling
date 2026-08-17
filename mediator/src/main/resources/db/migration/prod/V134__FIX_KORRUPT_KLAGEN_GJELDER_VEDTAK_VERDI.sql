-- Klagebehandling 019f40f4-d124-76a7-a1a7-7d29ca57e11e hadde fått lagret en korrupt
-- KLAGEN_GJELDER_VEDTAK-verdi med to UUID-er og manuelle merkelapper:
--   "019f1d91-b23b-7f16-9a80-65552a40602e (Meldekort)
--    019e8ce4-ae3c-7707-b23f-d21255ae5aaf (Vedtak)"
-- Dette gjorde at UUID.fromString() kastet "UUID string too large" ved oversendelse
-- til klageinstans (BehovbyggerKlageinstans.tilknyttedeJournalposter).
--
-- Klagen gjelder faktisk meldekortet, så Meldekort-UUID-en
-- (019f1d91-b23b-7f16-9a80-65552a40602e) er den korrekte behandlingId-en å bruke, og
-- settes derfor som eneste verdi.
--
-- OBS: Det finnes ingen rad i utsending_v1 med behandling_id lik denne UUID-en. Det betyr
-- at BehovbyggerKlageinstans.tilknyttedeJournalposter() ikke vil finne noen journalpost
-- for typen "OPPRINNELIG_VEDTAK" (finnUtsendingForBehandlingId returnerer null), og dette
-- feltet vil derfor utelates fra behovet til klageinstans.

UPDATE klage_v1
SET opplysninger = (
    SELECT jsonb_agg(
        CASE
            WHEN elem ->> 'type' = 'KLAGEN_GJELDER_VEDTAK'
                THEN jsonb_set(elem, '{verdi,value}', '"019f1d91-b23b-7f16-9a80-65552a40602e"')
            ELSE elem
        END
    )
    FROM jsonb_array_elements(opplysninger) AS elem
)
WHERE id = '019f40f4-d124-76a7-a1a7-7d29ca57e11e';
