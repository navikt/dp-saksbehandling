-- Kandidattabell for statistikkeksport. Erstatter kursoren som gikk på log.id, der rader kunne
-- gå tapt permanent fordi id tildeles ved objektkonstruksjon, ikke ved commit. En transaksjon
-- som committet sent havnet under kursoren og ble aldri sett igjen. Flagget vurdert gjør ingen
-- antakelse om rekkefølge.
CREATE TABLE IF NOT EXISTS statistikk_kandidat_v1
(
    tilstand_id          UUID                        NOT NULL PRIMARY KEY,
    vurdert              TIMESTAMP WITHOUT TIME ZONE,
    registrert_tidspunkt TIMESTAMP WITHOUT TIME ZONE DEFAULT timezone('Europe/Oslo'::text, current_timestamp)
);

CREATE INDEX IF NOT EXISTS idx_statistikk_kandidat_v1_ikke_vurdert
    ON statistikk_kandidat_v1 (tilstand_id)
    WHERE vurdert IS NULL;

-- StatistikkJob spør hvert 5. minutt etter rader som ikke er bekreftet levert til DVH.
-- Uten indeks blir det en seq scan av en tabell som vokser monotont.
-- Partiell indeks: bare de uoverførte er med, så indeksen holder seg liten selv når
-- tabellen har millioner av rader. Sorteringen på sekvensnummer ligger i indeksen,
-- så jobbens ORDER BY ... LIMIT blir en ren indeksskanning uten sorteringssteg.
CREATE INDEX IF NOT EXISTS idx_saksbehandling_statistikk_v1_ikke_overfort
    ON saksbehandling_statistikk_v1 (sekvensnummer)
    WHERE overfort_til_statistikk = FALSE;

