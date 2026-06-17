-- Uventet feil ved fatting av vedtak i Arena. Avbryter utsending.
UPDATE utsending_v1
SET    tilstand = 'Avbrutt'
WHERE  id       = '01992833-79c9-7257-85c2-0c382c7a2afe'
AND    tilstand = 'VenterPåVedtak';
