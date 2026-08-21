-- StatistikkJob spør hvert 5. minutt etter rader som ikke er bekreftet levert til DVH.
-- Uten indeks blir det en seq scan av en tabell som vokser monotont.
-- Partiell indeks: bare de uoverførte er med, så indeksen holder seg liten selv når
-- tabellen har millioner av rader. Sorteringen på sekvensnummer ligger i indeksen,
-- så jobbens ORDER BY ... LIMIT blir en ren indeksskanning uten sorteringssteg.
CREATE INDEX IF NOT EXISTS idx_saksbehandling_statistikk_v1_ikke_overfort
    ON saksbehandling_statistikk_v1 (sekvensnummer)
    WHERE overfort_til_statistikk = FALSE;
