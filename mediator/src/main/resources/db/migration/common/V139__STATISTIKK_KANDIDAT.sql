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
-- tabellen har millioner av rader.
CREATE INDEX IF NOT EXISTS idx_saksbehandling_statistikk_v1_ikke_overfort
    ON saksbehandling_statistikk_v1 (sekvensnummer)
    WHERE overfort_til_statistikk = FALSE;

-- Markeringen slår opp én rad om gangen på tilstand_id, opptil 1000 ganger per kjøring.
-- Uten indeks blir hvert oppslag en seq scan. Tabellen har ingen unik constraint på
-- tilstand_id (V89 droppet primærnøkkelen), så indeksen kan ikke være unik.
CREATE INDEX IF NOT EXISTS idx_saksbehandling_statistikk_v1_tilstand_id
    ON saksbehandling_statistikk_v1 (tilstand_id);

