-- Feil ved vurdering av minsteinntekt i Arena, medførte at vedtak ikke ble fattet.
-- Behandling må ferdigstilles med brev i Arena.
UPDATE  utsending_v1
SET     tilstand = 'Avbrutt'
WHERE   id = '0193b9e9-f1aa-7970-b0fa-ae23b86f38dc'
AND     tilstand = 'VenterPåVedtak';
