-- V35 and V74 used gen_random_uuid() (v4) instead of UUIDv7.
-- This script replaces them with pre-generated UUIDv7s based on the rows' tidspunkt.

-- Fix V35 row (tidspunkt: 2025-03-03 11:40:18.586938)
UPDATE  oppgave_tilstand_logg_v1
SET     id = '01955b97-a09a-7a5b-80f0-3f09df067482'
WHERE   id = '9b91bbfc-0bfa-4f31-86b5-1acff130f151'
;

-- Fix V74 row (tidspunkt: 2025-12-02 11:24:24.758851)
UPDATE  oppgave_tilstand_logg_v1
SET     id = '019ade97-8ab6-7a55-a1b6-b67819000104'
WHERE   id = '08b10ed6-c570-4b3a-9c1b-0fe96765ea81'
;
