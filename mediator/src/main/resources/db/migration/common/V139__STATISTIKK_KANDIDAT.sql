-- Overgang fra kursor til kandidattabell.
--
-- StatistikkJob fant tidligere nye rader med `log.id > siste_overførte`. Id-en er en UUIDv7 som
-- tildeles når Tilstandsendring konstrueres, ikke når raden committes, så en transaksjon som
-- committer sent havner permanent under kursoren. Kandidattabellen skrives i samme transaksjon
-- som tilstandsendringen og gjør ingen antakelse om rekkefølge.
CREATE TABLE IF NOT EXISTS statistikk_kandidat_v1
(
    tilstand_id          UUID                        NOT NULL PRIMARY KEY,
    vurdert              TIMESTAMP WITHOUT TIME ZONE,
    registrert_tidspunkt TIMESTAMP WITHOUT TIME ZONE DEFAULT timezone('Europe/Oslo'::text, current_timestamp)
);

-- Seeder kandidattabellen med tilstandsendringer som ennå ikke er eksportert, slik at den tar
-- over der kursoren slapp.
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

-- Indeksene lages etter seedingen, slik at de bygges én gang i stedet for å vedlikeholdes
-- underveis i innsettingen.

-- Jobben plukker kandidater som ennå ikke er vurdert. Partiell indeks holder den liten selv
-- når tabellen vokser, siden de vurderte faller ut.
CREATE INDEX IF NOT EXISTS idx_statistikk_kandidat_v1_ikke_vurdert
    ON statistikk_kandidat_v1 (tilstand_id)
    WHERE vurdert IS NULL;

-- StatistikkJob spør hvert 5. minutt etter rader som ikke er bekreftet levert til DVH.
-- Uten indeks blir det en seq scan av en tabell som vokser monotont.
CREATE INDEX IF NOT EXISTS idx_saksbehandling_statistikk_v1_ikke_overfort
    ON saksbehandling_statistikk_v1 (sekvensnummer)
    WHERE overfort_til_statistikk = FALSE;

-- Markeringen slår opp én rad om gangen på tilstand_id, opptil 1000 ganger per kjøring.
-- Uten indeks blir hvert oppslag en seq scan. Tabellen har ingen unik constraint på
-- tilstand_id (V89 droppet primærnøkkelen), så indeksen kan ikke være unik.
CREATE INDEX IF NOT EXISTS idx_saksbehandling_statistikk_v1_tilstand_id
    ON saksbehandling_statistikk_v1 (tilstand_id);
