-- Det ble feilaktig valgt brev i dp-sak på en meldekortbehandling. Avbryter utsending.
UPDATE utsending_v1
SET    tilstand = 'Avbrutt'
WHERE  id       = '019b20f3-f589-7ab5-a3be-2ce0bf3e16f6'
AND    tilstand = 'VenterPåVedtak';
