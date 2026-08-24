-- Overgang fra kursor til kandidattabell i prod.
--
-- Fyller statistikk_kandidat_v1 med de tilstandsendringene som ennå ikke er eksportert, slik at
-- kandidattabellen tar over der kursoren slapp.
--
-- Avgrensningen på behandling_id er den samme som uttrekket i StatistikkJob bruker, slik at vi
-- ikke drar inn historikk eksporten aldri har vært ment å dekke.
--
-- NOT EXISTS gjør migrasjonen uavhengig av når den kjører: rader som allerede ligger i
-- saksbehandling_statistikk_v1 utelates, så ingenting sendes til DVH på nytt, og rader som
-- kommer til mens utrullingen pågår fanges opp i stedet for å falle mellom stolene.
-- Derfor kreves det heller ikke at StatistikkJob er stoppet mens migrasjonen kjører.
INSERT INTO statistikk_kandidat_v1 (tilstand_id)
SELECT log.id
FROM oppgave_tilstand_logg_v1 log
         JOIN oppgave_v1 opp ON opp.id = log.oppgave_id
WHERE opp.behandling_id >= '019928dc-f521-7723-8ff6-f07154f5097d'
  AND NOT EXISTS (SELECT 1
                  FROM saksbehandling_statistikk_v1 s
                  WHERE s.tilstand_id = log.id)
ON CONFLICT DO NOTHING;
