-- Sletter utsendinger som har blitt opprettet uten å rulles tilbake når brevproduksjon feilet pga at regelmotor døde.
-- Behandlingsresultat vil bli resendt med eventname dp_saksbehandling_behandlingsresultat_utsending_retry.
DELETE
FROM    utsending_v1
WHERE   id              IN ('01a1eb60-fafb-7007-9f3a-16ad1fb962b0','01a1ebb0-b0b4-731d-9b63-d5b3c6481eb8')
AND     behandling_id   IN ('01a1eb60-eea7-7579-8d4c-3239d18e857d','01a1ebac-02c1-7637-8c17-81827bcd3e04')
AND     tilstand        = 'VenterPåVedtak'
;
