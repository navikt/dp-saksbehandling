-- Pga. feil ved publisering av meldinger ligger to hendelser i saksbehandling_statistikk_v1 uten å ha blitt publisert.
-- Disse må fjernes for at jobben skal kunne starte, da det ved jobb-oppstart sjekkes om det finnes upubliserte
-- rader i tabellen. Disse radene skal hentes på nytt.
-- Resetter sekvensnummeret til det høyeste nåværende + 1 for å unngå hull i sekvensen ved innsetting av nye rader.
DELETE
FROM    saksbehandling_statistikk_v1
WHERE   sekvensnummer IN (202664, 202665)
AND     tilstand_id   IN ('019e830b-41ba-7672-9273-e6f4c86c7fd8', '019e830b-41ba-7672-9273-e6f4c86c7fd7');

SELECT  setval(pg_get_serial_sequence('saksbehandling_statistikk_v1', 'sekvensnummer'), COALESCE(MAX(sekvensnummer)+1, 1), false)
FROM    saksbehandling_statistikk_v1;
