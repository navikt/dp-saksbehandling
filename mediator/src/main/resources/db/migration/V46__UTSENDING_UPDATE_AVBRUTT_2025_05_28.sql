-- Feil ved fatting av vedtak i Arena. Oppgaven er tatt i Arena.
UPDATE utsending_v1
SET    tilstand = 'Avbrutt'
WHERE  id       = '019715b4-b1b1-7925-80ee-0fec0d1ca5cb'
AND    tilstand = 'VenterPåVedtak';
