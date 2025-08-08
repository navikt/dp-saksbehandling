-- Feil ved fatting av vedtak i Arena. Bruker utensteng fra Arena.  Oppgaven er tatt i Arena.
UPDATE utsending_v1
SET    tilstand = 'Avbrutt'
WHERE  id       = '019888c2-c449-74a8-9416-e7e8d5a5e7a4'
AND    tilstand = 'VenterPåVedtak';
