ALTER TABLE person_v1 ALTER COLUMN registrert_tidspunkt TYPE TIMESTAMP WITH TIME ZONE USING registrert_tidspunkt AT TIME ZONE 'UTC';
ALTER TABLE person_v1 ALTER COLUMN endret_tidspunkt TYPE TIMESTAMP WITH TIME ZONE USING endret_tidspunkt AT TIME ZONE 'UTC';

ALTER TABLE behandling_v1 ALTER COLUMN registrert_tidspunkt TYPE TIMESTAMP WITH TIME ZONE USING registrert_tidspunkt AT TIME ZONE 'UTC';
ALTER TABLE behandling_v1 ALTER COLUMN endret_tidspunkt TYPE TIMESTAMP WITH TIME ZONE USING endret_tidspunkt AT TIME ZONE 'UTC';

ALTER TABLE oppgave_v1 ALTER COLUMN registrert_tidspunkt TYPE TIMESTAMP WITH TIME ZONE USING registrert_tidspunkt AT TIME ZONE 'UTC';
ALTER TABLE oppgave_v1 ALTER COLUMN endret_tidspunkt TYPE TIMESTAMP WITH TIME ZONE USING endret_tidspunkt AT TIME ZONE 'UTC';

ALTER TABLE emneknagg_v1 ALTER COLUMN registrert_tidspunkt TYPE TIMESTAMP WITH TIME ZONE USING registrert_tidspunkt AT TIME ZONE 'UTC';
ALTER TABLE emneknagg_v1 ALTER COLUMN endret_tidspunkt TYPE TIMESTAMP WITH TIME ZONE USING endret_tidspunkt AT TIME ZONE 'UTC';
