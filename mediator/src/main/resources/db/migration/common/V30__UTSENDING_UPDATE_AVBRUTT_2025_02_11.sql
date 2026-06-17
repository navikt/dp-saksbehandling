-- Feil som følge av av manglende funksjonalitet i Arena.
-- formidlingsgruppe kan ikke være IARBS da det ikke vil opprettes meldekort.
-- Midlertidig løsning er at saksbehandlerne ikke behandler disse sakene der bruker er registrert som
-- arbeidssøker, men har annen formidlingsgruppe enn ARBS i Arena.
UPDATE utsending_v1
SET    tilstand = 'Avbrutt'
WHERE  id       = '0194f032-8a9d-7fbe-9884-4a302fde7cb9'
AND    tilstand = 'VenterPåVedtak';
