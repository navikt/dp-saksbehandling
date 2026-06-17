ALTER TABLE behandling_v1 DISABLE TRIGGER oppdater_endret_tidspunkt;

UPDATE behandling_v1
SET    behandling_type = 'MANUELL'
WHERE  behandling_type = 'RETT_TIL_DAGPENGER'
AND EXISTS(
    SELECT 1
    FROM   hendelse_v1 hend
    WHERE  hend.behandling_id = behandling_v1.id
    AND    hend.hendelse_type = 'ManuellBehandlingOpprettetHendelse'
);

UPDATE behandling_v1
SET    behandling_type = 'MELDEKORT'
WHERE  behandling_type = 'RETT_TIL_DAGPENGER'
AND EXISTS(
    SELECT 1
    FROM   hendelse_v1 hend
    WHERE  hend.behandling_id = behandling_v1.id
    AND    hend.hendelse_type = 'MeldekortbehandlingOpprettetHendelse'
);

UPDATE behandling_v1
SET    behandling_type = 'SØKNAD'
WHERE  behandling_type = 'RETT_TIL_DAGPENGER';

ALTER TABLE behandling_v1 ENABLE TRIGGER oppdater_endret_tidspunkt;

ALTER TABLE behandling_v1
RENAME COLUMN behandling_type TO utlost_av;
