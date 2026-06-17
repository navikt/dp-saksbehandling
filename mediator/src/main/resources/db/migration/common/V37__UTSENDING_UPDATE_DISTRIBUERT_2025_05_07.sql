-- Utsending er allerede distribuert. Setter tilstand til Distribuert.
UPDATE utsending_v1
SET    tilstand = 'Distribuert'
WHERE  id       = '0196a9ce-2ccc-7b78-ac51-0b467a6d987a'
AND    tilstand = 'AvventerDistribuering';
