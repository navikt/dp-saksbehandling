-- Feil ved fatting av vedtak i Arena. Oppgaven er tatt i Arena.
UPDATE utsending_v1
SET    tilstand = 'Avbrutt'
WHERE  id       = '0196c99a-88ee-7aab-bed6-d47ed610a256'
AND    tilstand = 'VenterPåVedtak';
