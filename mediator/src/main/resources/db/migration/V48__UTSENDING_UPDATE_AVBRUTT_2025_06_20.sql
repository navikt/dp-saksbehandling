-- Feil ved fatting av vedtak i Arena. Oppgaven er tatt i Arena.
UPDATE utsending_v1
SET    tilstand = 'Avbrutt'
WHERE  id       = '01978800-171d-7c2b-8412-67646b77d3b1'
AND    tilstand = 'VenterPåVedtak';
