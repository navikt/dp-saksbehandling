-- Overgang fra kursor til kandidattabell i dev.
--
-- Fyller statistikk_kandidat_v1 med tilstandsendringer som ennå ikke er eksportert, slik at
-- kandidattabellen tar over der kursoren slapp.
--
-- Forutsetter at StatistikkJob er stoppet mens dette kjører, slik at ingenting skrives til
-- saksbehandling_statistikk_v1 samtidig.
--
-- TODO AVKLAR: bytt ut id-en under før deploy. Finn den ved å kjøre denne mot dev med jobben
--  stoppet — den gir laveste tilstandsendring som ennå ikke er eksportert, altså der kursoren
--  reelt sett står:
--
--    SELECT min(log.id)
--    FROM   oppgave_tilstand_logg_v1 log
--    JOIN   oppgave_v1               opp ON opp.id = log.oppgave_id
--    WHERE  opp.behandling_id >= '019928dc-f521-7723-8ff6-f07154f5097d'
--    AND    NOT EXISTS (SELECT 1
--                       FROM   saksbehandling_statistikk_v1 s
--                       WHERE  s.tilstand_id = log.id);
--
INSERT INTO statistikk_kandidat_v1 (tilstand_id)
SELECT id
FROM oppgave_tilstand_logg_v1
WHERE id > '00000000-0000-0000-0000-000000000000'
ON CONFLICT DO NOTHING;
