-- Flyttet fra V137 til V138: main tok i bruk versjon 137 til
-- prod/V137__UPDATE_OPPGAVE_SISTE_SAKSBEHANDLER_2026_08_21.sql. To migrasjoner med samme
-- versjon får Flyway til å avbryte før noe kjøres.
--
-- IF NOT EXISTS fordi kolonnen allerede ble lagt til i dev da skriptet kjørte som V137.
ALTER TABLE saksbehandling_statistikk_v1
ADD COLUMN IF NOT EXISTS relatert_behandling_id UUID REFERENCES behandling_v1 (id) ON DELETE CASCADE
;
