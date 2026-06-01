CREATE TABLE IF NOT EXISTS noedbremset_person_v1
(
    person_id            UUID PRIMARY KEY REFERENCES person_v1 (id),
    registrert_tidspunkt TIMESTAMP WITHOUT TIME ZONE DEFAULT timezone('Europe/Oslo'::text, current_timestamp)
);

DO
$$
    BEGIN
        IF EXISTS
            (SELECT 1 FROM pg_roles WHERE rolname = 'cloudsqliamuser')
        THEN
            GRANT SELECT ON TABLE noedbremset_person_v1 TO cloudsqliamuser;
        END IF;
    END
$$;
