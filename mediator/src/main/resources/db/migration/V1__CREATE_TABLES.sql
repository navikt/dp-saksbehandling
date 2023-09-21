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
    person_ident VARCHAR(11)              NOT NULL REFERENCES person (ident),
    opprettet    TIMESTAMP WITH TIME ZONE NOT NULL,
    uuid         uuid                     NOT NULL UNIQUE,
    tilstand     TEXT                     NOT NULL,
    sak_id       uuid                     NOT NULL REFERENCES sak (uuid)
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
    parent_id     uuid NOT NULL REFERENCES steg (uuid),
    child_id      uuid NOT NULL REFERENCES steg (uuid),
    UNIQUE (behandling_id, parent_id, child_id)
);

CREATE TABLE IF NOT EXISTS sporing
(
    id          BIGSERIAL PRIMARY KEY,
    steg_uuid   uuid                     NOT NULL UNIQUE REFERENCES steg (uuid),
    utført      TIMESTAMP WITH TIME ZONE NOT NULL,
    begrunnelse TEXT                     NULL,
    utført_av   TEXT                     NULL,
    json        jsonb                    NULL,
    type        TEXT                     NOT NULL
);

CREATE TABLE IF NOT EXISTS oppgave
(
    id            BIGSERIAL PRIMARY KEY,
    uuid          uuid                     NOT NULL UNIQUE,
    opprettet     TIMESTAMP WITH TIME ZONE NOT NULL,
    utføres_av    TEXT                     NULL,
    behandling_id uuid                     NOT NULL REFERENCES behandling (uuid)
);