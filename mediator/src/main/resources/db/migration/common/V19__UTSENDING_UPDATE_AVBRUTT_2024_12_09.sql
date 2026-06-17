-- Feil ved vurdering av minsteinntekt i Arena, medførte at vedtak ikke ble fattet.
-- Behandling må ferdigstilles med brev i Arena.
UPDATE  utsending_v1
SET     tilstand = 'Avbrutt'
WHERE   id = '0193ab85-e18b-7a65-b384-51bba68bee30'
AND     tilstand = 'VenterPåVedtak';
