CREATE OR REPLACE FUNCTION oppdater_endret_tidspunkt()
RETURNS TRIGGER AS $$
BEGIN
    NEW.endret_tidspunkt = current_timestamp;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE IF NOT EXISTS person_v1
(
    id                    UUID PRIMARY KEY,
    ident                 VARCHAR(11)                 NOT NULL UNIQUE,
    registrert_tidspunkt  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    endret_tidspunkt      TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS person_ident_index ON person_v1 (ident);
CREATE TRIGGER oppdater_endret_tidspunkt
BEFORE UPDATE ON person_v1
FOR EACH ROW EXECUTE FUNCTION oppdater_endret_tidspunkt();

CREATE TABLE IF NOT EXISTS behandling_v1
(
    id                    UUID PRIMARY KEY,
    person_id             UUID                        NOT NULL REFERENCES person_v1 (id),
    opprettet             TIMESTAMP WITH TIME ZONE    NOT NULL,
    registrert_tidspunkt  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    endret_tidspunkt      TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS behandling_person_id_index ON behandling_v1 (person_id);
CREATE TRIGGER oppdater_endret_tidspunkt
BEFORE UPDATE ON behandling_v1
FOR EACH ROW EXECUTE FUNCTION oppdater_endret_tidspunkt();

CREATE TABLE IF NOT EXISTS oppgave_v1
(
    id                    UUID PRIMARY KEY,
    behandling_id         UUID                        NOT NULL REFERENCES behandling_v1 (id),
    tilstand              TEXT                        NOT NULL,
    opprettet             TIMESTAMP WITH TIME ZONE    NOT NULL,
    registrert_tidspunkt  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    endret_tidspunkt      TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS oppgave_behandling_id_index ON oppgave_v1 (behandling_id);
CREATE TRIGGER oppdater_endret_tidspunkt
BEFORE UPDATE ON oppgave_v1
FOR EACH ROW EXECUTE FUNCTION oppdater_endret_tidspunkt();

CREATE TABLE IF NOT EXISTS emneknagg_v1
(
    id                    BIGSERIAL PRIMARY KEY,
    oppgave_id            UUID                        NOT NULL REFERENCES oppgave_v1 (id),
    emneknagg             TEXT                        NOT NULL,
    registrert_tidspunkt  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    endret_tidspunkt      TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS emneknagg_oppgave_id_index ON emneknagg_v1 (oppgave_id);
ALTER TABLE emneknagg_v1 ADD CONSTRAINT emneknagg_oppgave_unique UNIQUE (oppgave_id, emneknagg);
CREATE TRIGGER oppdater_endret_tidspunkt
BEFORE UPDATE ON emneknagg_v1
FOR EACH ROW EXECUTE FUNCTION oppdater_endret_tidspunkt();
