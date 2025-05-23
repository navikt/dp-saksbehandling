-- Feil ved fatting av vedtak i Arena. Personen ikke i formidlingsgruppe; Arbeidssøker.
UPDATE utsending_v1
SET    tilstand = 'Avbrutt'
WHERE  id       = '0196f748-44ff-7a0d-b588-017144a494cc'
AND    tilstand = 'VenterPåVedtak';
