-- Feil ved fatting av vedtak i Arena. Oppgaven er tatt i Arena.
UPDATE utsending_v1
SET    tilstand = 'Avbrutt'
WHERE  id       = '0197c07f-9cfa-7fa3-8be3-b6be028fd002'
AND    tilstand = 'VenterPåVedtak';
