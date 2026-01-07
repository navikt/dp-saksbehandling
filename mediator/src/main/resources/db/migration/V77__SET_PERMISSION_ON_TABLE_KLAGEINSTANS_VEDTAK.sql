DO
$$
BEGIN
        IF EXISTS
            (SELECT 1 FROM pg_roles WHERE rolname = 'cloudsqliamuser')
        THEN
            GRANT SELECT ON TABLE klageinstans_vedtak_v1 TO cloudsqliamuser;
END IF;
END
$$;
