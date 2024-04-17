ALTER TABLE oppgave_v1
    ADD COLUMN saksbehandler_ident TEXT;

CREATE INDEX IF NOT EXISTS oppgave_saksbehandler_ident_index ON oppgave_v1 (saksbehandler_ident);
