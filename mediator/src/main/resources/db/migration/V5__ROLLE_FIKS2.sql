
UPDATE steg SET rolle = 'Beslutter'
WHERE steg_id = 'Fatt vedtak'
  AND type = 'Prosess';

UPDATE steg SET rolle = 'Saksbehandler'
WHERE steg_id = 'Forslag til vedtak'
  AND type = 'Prosess';