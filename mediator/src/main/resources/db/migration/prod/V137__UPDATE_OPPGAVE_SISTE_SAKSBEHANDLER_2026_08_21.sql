-- Retter én prod-oppgave der tildelNesteOppgave satte siste_saksbehandler_ident til NULL
-- ved overgang fra KLAR_TIL_KONTROLL til UNDER_KONTROLL.
UPDATE  oppgave_v1
SET     siste_saksbehandler_ident = 'S157157'
WHERE   id = '019f36d2-0128-7742-96af-72ff51489b85'
AND     siste_saksbehandler_ident IS NULL;
