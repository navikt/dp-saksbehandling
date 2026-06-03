CREATE TABLE outbox
(
    id         BIGSERIAL PRIMARY KEY,
    key        TEXT                     NOT NULL,
    message    TEXT                     NOT NULL,
    status     TEXT                     NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT timezone('Europe/Oslo'::text, current_timestamp)
);

CREATE INDEX idx_outbox_pending ON outbox (id) WHERE status = 'PENDING';

DO
$$
BEGIN
        IF EXISTS
            (SELECT 1 FROM pg_roles WHERE rolname = 'cloudsqliamuser')
        THEN
            GRANT SELECT ON TABLE nodbremset_person_v1 TO cloudsqliamuser;
END IF;
END
$$;
