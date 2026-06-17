-- Feil i dp-behandling. Utsendingene distribuert av saksbehandlere
-- i Arena.
UPDATE utsending_v1
SET tilstand = 'Avbrutt'
WHERE id IN
--PROD
(
'019390c2-a815-7fa9-80f8-a964ee0e2091',
'019390c1-86c9-7cde-8b7d-c916adc0c8dc',
'019390c0-786f-73e3-b871-c974188b1bd2',
'019390bc-8332-711c-8ecd-ed7103e1af02',
'019390b6-ceb7-744d-9014-bbf122cb33e3',
'019390b4-7fb3-7c50-b0ab-e7f7a0e42a3e',
'01939093-12d0-7b9c-bcb6-0fd66454acaf'
 );