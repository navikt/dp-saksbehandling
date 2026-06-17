-- Feil som følge av manglende funksjonalitet i Arena fører til at bruker har formidlingsgruppe "Ikke arbeidssøker".
-- Formidlingsgruppe kan ikke være "Ikke arbeidssøker", da det ikke vil opprettes meldekort.
-- Midlertidig løsning er at saksbehandlerne må håndtere dette manuelt i Arena. Avbryter utsendingen her.
UPDATE utsending_v1
SET    tilstand = 'Avbrutt'
WHERE  id       = '0195709c-5dbf-7617-84c7-d44a96b06858'
AND    tilstand = 'VenterPåVedtak';
