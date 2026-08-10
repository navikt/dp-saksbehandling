UPDATE  oppgave_v1
SET     siste_saksbehandler_ident = NULL
WHERE   tilstand = 'PAA_VENT'
AND     behandler_ident IS NULL
AND     siste_saksbehandler_ident IS NOT NULL;
