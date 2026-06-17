-- Setter sak som dp-sak pga. bug i BehandlingsresultatMottakForSak, som ikke fanget opp at vedtaket var en innvilgelse.
UPDATE sak_v2
SET    er_dp_sak = true
WHERE  id = '019a493f-af19-7502-82a2-b18fb275a877';
