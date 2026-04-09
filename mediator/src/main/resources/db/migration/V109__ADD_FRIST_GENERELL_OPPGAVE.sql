-- Legg til frist-kolonne for generelle oppgaver
-- Når frist er satt, opprettes Oppgave i PåVent-tilstand med utsattTil = frist
ALTER TABLE generell_oppgave_v1 ADD COLUMN frist DATE;

COMMENT ON COLUMN generell_oppgave_v1.frist IS 'Frist for oppgaven. Oppgave settes til PåVent med utsattTil = frist';
