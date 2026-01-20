-- Feil ved fatting av vedtak i Arena. Avbryter utsending.
UPDATE utsending_v1
SET    tilstand = 'Avbrutt'
WHERE  id       = '019bd649-8eb9-74a6-8a17-c7f136ce138d'
AND    tilstand = 'VenterPåVedtak';
