-- Det ble feilaktig valgt brev i dp-sak på en meldekortbehandling. Avbryter utsending.
UPDATE utsending_v1
SET    tilstand = 'Avbrutt'
WHERE  id       = '019a5d73-5270-7afa-b2d6-2fbdec2ed7c8'
AND    tilstand = 'VenterPåVedtak';
