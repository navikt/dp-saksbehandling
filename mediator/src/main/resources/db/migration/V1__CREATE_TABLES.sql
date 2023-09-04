CREATE TABLE IF NOT EXISTS person
(
    id    BIGSERIAL PRIMARY KEY,
    ident VARCHAR(11) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS sak
(
    id           BIGSERIAL PRIMARY KEY,
    uuid         uuid        NOT NULL UNIQUE,
    person_ident VARCHAR(11) NOT NULL REFERENCES person (ident)
);

CREATE TABLE IF NOT EXISTS behandling
(
    id           BIGSERIAL PRIMARY KEY,
    person_ident VARCHAR(11)                                                       NOT NULL REFERENCES person (ident),
    opprettet    TIMESTAMP WITH TIME ZONE DEFAULT (NOW() AT TIME ZONE 'utc'::TEXT) NOT NULL,
    uuid         uuid                                                              NOT NULL UNIQUE,
    tilstand     TEXT                                                              NOT NULL,
    sak_id       uuid                                                              NOT NULL REFERENCES sak (uuid)
);

CREATE TABLE IF NOT EXISTS steg
(
    id              BIGSERIAL PRIMARY KEY,
    behandling_uuid uuid    NOT NULL REFERENCES behandling (uuid),
    uuid            uuid    NOT NULL UNIQUE,
    steg_id         TEXT    NOT NULL,
    tilstand        TEXT    NOT NULL,
    type            TEXT    NOT NULL,
    svar_type       TEXT    NOT NULL,
    ubesvart        BOOLEAN NOT NULL,
    string          TEXT    NULL,
    dato            DATE    NULL,
    heltall         INT     NULL,
    boolsk          BOOLEAN NULL,
    desimal         FLOAT   NULL
);


CREATE TABLE IF NOT EXISTS steg_relasjon
(

    behandling_id uuid NOT NULL REFERENCES behandling (uuid),
    parent_id       UUID NOT NULL REFERENCES steg (uuid),
    child_id        UUID NOT NULL REFERENCES steg (uuid),
    UNIQUE (behandling_id, parent_id, child_id)
);

CREATE TABLE IF NOT EXISTS sporing
(
    id          BIGSERIAL PRIMARY KEY,
    steg        uuid                     NOT NULL REFERENCES steg (uuid),
    utført      TIMESTAMP WITH TIME ZONE NOT NULL,
    begrunnelse TEXT                     NULL,
    utførtav    TEXT                     NULL,
    json        jsonb                    NULL,
    type        TEXT                     NOT NULL
);

-- CREATE TABLE IF NOT EXISTS aktivitet
-- (
--     id           BIGSERIAL PRIMARY KEY,
--     uuid         uuid                                                              NOT NULL UNIQUE,
--     person_ident VARCHAR(11)                                                       NOT NULL REFERENCES person (ident),
--     tilstand     TEXT                                                              NOT NULL,
--     opprettet    TIMESTAMP WITH TIME ZONE DEFAULT (NOW() AT TIME ZONE 'utc'::TEXT) NOT NULL,
--     dato         DATE                                                              NOT NULL,
--     "type"       TEXT                                                              NOT NULL,
--     tid          INTERVAL                                                          NOT NULL
-- );
--
-- CREATE TABLE IF NOT EXISTS rapporteringsperiode
-- (
--     id             BIGSERIAL PRIMARY KEY,
--     uuid           uuid                                                              NOT NULL UNIQUE,
--     person_ident   VARCHAR(11)                                                       NOT NULL REFERENCES person (ident),
--     tilstand       TEXT                                                              NOT NULL,
--     opprettet      TIMESTAMP WITH TIME ZONE DEFAULT (NOW() AT TIME ZONE 'utc'::TEXT) NOT NULL,
--     beregnes_etter DATE                                                              NOT NULL,
--     fom            DATE                                                              NOT NULL,
--     tom            DATE                                                              NOT NULL,
--     korrigerer     uuid                                                              NULL REFERENCES rapporteringsperiode (uuid),
--     korrigert_av   uuid                                                              NULL REFERENCES rapporteringsperiode (uuid)
-- );
--
-- CREATE TABLE IF NOT EXISTS dag_aktivitet
-- (
--     rapporteringsperiode_id uuid NOT NULL REFERENCES rapporteringsperiode (uuid),
--     aktivitet_id            uuid NOT NULL REFERENCES aktivitet (uuid) ON DELETE CASCADE,
--     UNIQUE (rapporteringsperiode_id, aktivitet_id)
-- );
--
-- CREATE TABLE IF NOT EXISTS dag
-- (
--     id                      BIGSERIAL PRIMARY KEY,
--     rapporteringsperiode_id uuid NOT NULL REFERENCES rapporteringsperiode (uuid) ON DELETE CASCADE,
--     dato                    DATE NOT NULL,
--     strategi                TEXT NOT NULL,
--     UNIQUE (rapporteringsperiode_id, dato)
-- );
--
-- CREATE TABLE IF NOT EXISTS rapporteringsplikt
-- (
--     id          BIGSERIAL PRIMARY KEY,
--     uuid        uuid                                                              NOT NULL UNIQUE,
--     person_id   BIGSERIAL                                                         NOT NULL REFERENCES person (id),
--     type        TEXT                                                              NOT NULL,
--     opprettet   TIMESTAMP WITH TIME ZONE DEFAULT (NOW() AT TIME ZONE 'utc'::TEXT) NOT NULL,
--     gjelder_fra TIMESTAMP WITH TIME ZONE DEFAULT (NOW() AT TIME ZONE 'utc'::TEXT) NOT NULL
-- );
--
-- CREATE TABLE godkjenningsendring
-- (
--     id                      BIGSERIAL PRIMARY KEY,
--     uuid                    uuid UNIQUE              NOT NULL,
--     rapporteringsperiode_id uuid                     NOT NULL REFERENCES rapporteringsperiode (uuid) ON DELETE CASCADE,
--     opprettet               TIMESTAMP WITH TIME ZONE NOT NULL,
--     avgodkjent_av           BIGINT                   NULL REFERENCES godkjenningsendring (id) ON DELETE CASCADE,
--     begrunnelse             TEXT,
--     utfort_kilde            TEXT                     NOT NULL,
--     utfort_id               TEXT                     NOT NULL
-- );
