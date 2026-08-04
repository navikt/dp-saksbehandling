ALTER TABLE oppgave_v1
ADD COLUMN siste_saksbehandler_ident TEXT,
ADD COLUMN siste_beslutter_ident TEXT
;
ALTER TABLE oppgave_v1
RENAME COLUMN saksbehandler_ident TO behandler_ident
;
