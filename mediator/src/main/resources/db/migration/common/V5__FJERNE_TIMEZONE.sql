DROP TRIGGER IF EXISTS oppdater_endret_tidspunkt ON behandling_v1;
DROP TRIGGER IF EXISTS oppdater_endret_tidspunkt ON emneknagg_v1;
DROP TRIGGER IF EXISTS oppdater_endret_tidspunkt ON oppgave_v1;
DROP TRIGGER IF EXISTS oppdater_endret_tidspunkt ON person_v1;


ALTER table behandling_v1
    ADD COLUMN opprettet2            TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN registrert_tidspunkt2 TIMESTAMP WITHOUT TIME ZONE DEFAULT timezone('Europe/Oslo'::text, current_timestamp),
    ADD COLUMN endret_tidspunkt2     TIMESTAMP WITHOUT TIME ZONE DEFAULT timezone('Europe/Oslo'::text, current_timestamp);

ALTER table oppgave_v1
    ADD COLUMN opprettet2            TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN registrert_tidspunkt2 TIMESTAMP WITHOUT TIME ZONE DEFAULT timezone('Europe/Oslo'::text, current_timestamp),
    ADD COLUMN endret_tidspunkt2     TIMESTAMP WITHOUT TIME ZONE DEFAULT timezone('Europe/Oslo'::text, current_timestamp);

ALTER table person_v1
    ADD COLUMN registrert_tidspunkt2 TIMESTAMP WITHOUT TIME ZONE DEFAULT timezone('Europe/Oslo'::text, current_timestamp),
    ADD COLUMN endret_tidspunkt2     TIMESTAMP WITHOUT TIME ZONE DEFAULT timezone('Europe/Oslo'::text, current_timestamp);

ALTER table emneknagg_v1
    ADD COLUMN registrert_tidspunkt2 TIMESTAMP WITHOUT TIME ZONE DEFAULT timezone('Europe/Oslo'::text, current_timestamp),
    ADD COLUMN endret_tidspunkt2     TIMESTAMP WITHOUT TIME ZONE DEFAULT timezone('Europe/Oslo'::text, current_timestamp);


UPDATE behandling_v1
SET opprettet2            = opprettet::timestamp with time zone at time zone 'Europe/Oslo',
    registrert_tidspunkt2 = registrert_tidspunkt::timestamp with time zone at time zone 'Europe/Oslo',
    endret_tidspunkt2     = endret_tidspunkt::timestamp with time zone at time zone 'Europe/Oslo'
WHERE 1 = 1;

UPDATE oppgave_v1
SET opprettet2            = opprettet::timestamp with time zone at time zone 'Europe/Oslo',
    registrert_tidspunkt2 = registrert_tidspunkt::timestamp with time zone at time zone 'Europe/Oslo',
    endret_tidspunkt2     = endret_tidspunkt::timestamp with time zone at time zone 'Europe/Oslo'
WHERE 1 = 1;

UPDATE person_v1
SET registrert_tidspunkt2 = registrert_tidspunkt::timestamp with time zone at time zone 'Europe/Oslo',
    endret_tidspunkt2     = endret_tidspunkt::timestamp with time zone at time zone 'Europe/Oslo'
WHERE 1 = 1;

UPDATE emneknagg_v1
SET registrert_tidspunkt2 = registrert_tidspunkt::timestamp with time zone at time zone 'Europe/Oslo',
    endret_tidspunkt2     = endret_tidspunkt::timestamp with time zone at time zone 'Europe/Oslo'
WHERE 1 = 1;

ALTER table behandling_v1
    DROP COLUMN opprettet,
    DROP COLUMN registrert_tidspunkt,
    DROP COLUMN endret_tidspunkt;

ALTER table oppgave_v1
    DROP COLUMN opprettet,
    DROP COLUMN registrert_tidspunkt,
    DROP COLUMN endret_tidspunkt;

ALTER table person_v1
    DROP COLUMN registrert_tidspunkt,
    DROP COLUMN endret_tidspunkt;

ALTER table emneknagg_v1
    DROP COLUMN registrert_tidspunkt,
    DROP COLUMN endret_tidspunkt;


ALTER table behandling_v1
    RENAME COLUMN opprettet2 TO opprettet;
ALTER table behandling_v1
    RENAME COLUMN registrert_tidspunkt2 TO registrert_tidspunkt;
ALTER table behandling_v1
    RENAME COLUMN endret_tidspunkt2 TO endret_tidspunkt;

ALTER table oppgave_v1
    RENAME COLUMN opprettet2 TO opprettet;
ALTER table oppgave_v1
    RENAME COLUMN registrert_tidspunkt2 TO registrert_tidspunkt;
ALTER table oppgave_v1
    RENAME COLUMN endret_tidspunkt2 TO endret_tidspunkt;

ALTER table person_v1
    RENAME COLUMN registrert_tidspunkt2 TO registrert_tidspunkt;
ALTER table person_v1
    RENAME COLUMN endret_tidspunkt2 TO endret_tidspunkt;

ALTER table emneknagg_v1
    RENAME COLUMN registrert_tidspunkt2 TO registrert_tidspunkt;
ALTER table emneknagg_v1
    RENAME COLUMN endret_tidspunkt2 TO endret_tidspunkt;

ALTER table behandling_v1
    ALTER COLUMN opprettet SET NOT NULL,
    ALTER COLUMN registrert_tidspunkt SET NOT NULL,
    ALTER COLUMN endret_tidspunkt SET NOT NULL;

ALTER table oppgave_v1
    ALTER COLUMN opprettet SET NOT NULL,
    ALTER COLUMN registrert_tidspunkt SET NOT NULL,
    ALTER COLUMN endret_tidspunkt SET NOT NULL;

ALTER table person_v1
    ALTER COLUMN registrert_tidspunkt SET NOT NULL,
    ALTER COLUMN endret_tidspunkt SET NOT NULL;

ALTER table emneknagg_v1
    ALTER COLUMN registrert_tidspunkt SET NOT NULL,
    ALTER COLUMN endret_tidspunkt SET NOT NULL;


CREATE OR REPLACE FUNCTION oppdater_endret_tidspunkt()
    RETURNS TRIGGER AS
$$
BEGIN
    NEW.endret_tidspunkt = timezone('Europe/Oslo'::text, current_timestamp);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


CREATE TRIGGER oppdater_endret_tidspunkt
    BEFORE UPDATE
    ON behandling_v1
    FOR EACH ROW
EXECUTE FUNCTION oppdater_endret_tidspunkt();

CREATE TRIGGER oppdater_endret_tidspunkt
    BEFORE UPDATE
    ON oppgave_v1
    FOR EACH ROW
EXECUTE FUNCTION oppdater_endret_tidspunkt();

CREATE TRIGGER oppdater_endret_tidspunkt
    BEFORE UPDATE
    ON person_v1
    FOR EACH ROW
EXECUTE FUNCTION oppdater_endret_tidspunkt();

CREATE TRIGGER oppdater_endret_tidspunkt
    BEFORE UPDATE
    ON emneknagg_v1
    FOR EACH ROW
EXECUTE FUNCTION oppdater_endret_tidspunkt();
