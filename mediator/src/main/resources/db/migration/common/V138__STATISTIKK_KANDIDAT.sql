-- Kandidater til statistikkeksport.
--
-- Tabellen svarer på ett spørsmål: hvilke tilstandsendringer har statistikkjobben ennå ikke sett
-- på? Den er ikke en utboks — en rad her betyr ikke at noe skal sendes. De fleste kandidater
-- filtreres bort av jobbens spørring (Oppfølging, og rader under gulvene) og markeres som vurdert
-- uten at noe eksporteres. Merk at repoet allerede har en ekte utboks i kafka_utboks_v1; dette er
-- noe annet.
--
-- Problemet: jobben fant nye tilstandsendringer med kursoren `log.id > siste_overførte`.
-- `oppgave_tilstand_logg_v1.id` er en UUIDv7 som settes i applikasjonskoden når objektet
-- konstrueres, mens raden først blir synlig ved COMMIT. To transaksjoner kan derfor committe i
-- motsatt rekkefølge av id-ene sine. Committer A (lav id) etter at B (høy id) er lest og kursoren
-- flyttet til B, blir A liggende permanent under kursoren og sendes aldri.
--
-- Løsningen: hver tilstandsendring får en rad her, skrevet i samme transaksjon som selve
-- tilstandsendringen. Da finnes raden nøyaktig når tilstandsendringen finnes, og rekkefølgen
-- mellom transaksjoner spiller ingen rolle.
--
-- Raden inneholder bare nøkkelen. Alle opplysninger hentes fortsatt av jobbens spørring ved
-- eksport, akkurat som før.
--
-- Bevisst uten fremmednøkkel mot oppgave_tilstand_logg_v1. En fremmednøkkel ville latt denne
-- tabellen abortere transaksjonen som lagrer tilstandsendringen, altså latt statistikk velte
-- saksbehandling. Dette er et sidespor og skal aldri kunne det.
--
-- Prisen er at en rad her i prinsippet kan peke på en tilstandsendring som ikke finnes, for
-- eksempel etter en oppryddingsmigrasjon som sletter fra loggen (se V86, V88, V115). Det er
-- harmløst: jobbens spørring joiner mot loggen, finner ingenting, og raden markeres som vurdert
-- uten at noe eksporteres.
CREATE TABLE IF NOT EXISTS statistikk_kandidat_v1
(
    tilstand_id          UUID                        NOT NULL PRIMARY KEY,
    vurdert              TIMESTAMP WITHOUT TIME ZONE,
    registrert_tidspunkt TIMESTAMP WITHOUT TIME ZONE DEFAULT timezone('Europe/Oslo'::text, current_timestamp)
);

-- Jobben spør kun etter rader som ikke er vurdert. Partiell indeks gjør at kostnaden følger
-- antall uvurderte rader, ikke tabellens totale størrelse.
CREATE INDEX IF NOT EXISTS idx_statistikk_kandidat_v1_ikke_vurdert
    ON statistikk_kandidat_v1 (tilstand_id)
    WHERE vurdert IS NULL;

-- Selve overgangen fra kursor til kandidattabell er miljøspesifikk, fordi kursoren står på
-- forskjellig sted i dev og prod. Se V139 i db/migration/dev og db/migration/prod.
--
-- Grant er bevisst utelatt: V122 setter ALTER DEFAULT PRIVILEGES i begge miljøer, så nye tabeller
-- får riktige rettigheter automatisk (samme mønster som V129).
