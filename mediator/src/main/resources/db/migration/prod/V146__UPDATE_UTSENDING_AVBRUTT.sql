-- Avbryter utsending av stans-melding til død person. Skal få omgjøring i stedet.
UPDATE  utsending_v1
SET     tilstand        = 'Avbrutt'
WHERE   id              = '01a1c44d-0eb1-747e-8b8f-268725a88d6a'
AND     behandling_id   = '01a1c2f8-2009-7222-998b-3e39ee761c92'
AND     tilstand        = 'AvventerDistribuering'
;