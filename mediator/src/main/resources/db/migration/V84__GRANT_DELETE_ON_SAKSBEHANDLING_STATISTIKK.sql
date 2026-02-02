DO
$$
    BEGIN
        IF EXISTS
            (SELECT 1 FROM pg_roles WHERE rolname = 'cloudsqliamuser')
        THEN
            GRANT DELETE ON TABLE saksbehandling_statistikk_v1 TO cloudsqliamuser;
        END IF;
    END
$$;
