-- Kallet til dp-behandling for å opprette revurdering feilet med connect timeout, og oppfølgingene ble stående i FERDIGSTILL_STARTET.
-- Fra FERDIGSTILL_STARTET kan saksbehandler verken ferdigstille eller opprette revurdering på nytt.
-- Ingen behandling ble opprettet i dp-behandling, så vi setter oppfølgingene tilbake til BEHANDLES.
UPDATE  oppfolging_v1
SET     tilstand                = 'BEHANDLES'
WHERE   id                      IN ('01a1d53a-d5f1-739a-be32-0c1811bc3fe8', '01a217a9-a93b-7440-8fd9-56f29c6ef685')
AND     tilstand                = 'FERDIGSTILL_STARTET'
AND     resultat_behandling_id  IS NULL
;
