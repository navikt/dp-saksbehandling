CREATE TABLE IF NOT EXISTS hendelse
(
    id                   BIGSERIAL PRIMARY KEY,
    behandling_id        UUID        NOT NULL REFERENCES behandling (uuid),
    melding_referanse_id UUID UNIQUE NOT NULL,
    clazz                TEXT        NOT NULL,
    soknad_id            UUID        NULL DEFAULT NULL,
    journalpost_id       TEXT        NULL DEFAULT NULL,
    oppgave_id           UUID        NULL DEFAULT NULL
);
