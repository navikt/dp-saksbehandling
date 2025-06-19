-- Feil ved fatting av vedtak i Arena. Oppgaven er tatt i Arena.
UPDATE utsending_v1
SET    tilstand = 'Avbrutt'
WHERE  id       IN( '019782b9-ad68-7508-be75-b58796f4c831','0197827a-02c0-7d1d-b8ee-58030e4229ae' )
AND    tilstand = 'VenterPåVedtak';
