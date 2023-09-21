CREATE TABLE IF NOT EXISTS hendelse
(
    id                   BIGSERIAL PRIMARY KEY,
    behandling_id        BIGINT                   NOT NULL REFERENCES behandling (id),
    melding_referanse_id UUID UNIQUE              NOT NULL,
    opprettet            TIMESTAMP WITH TIME ZONE NOT NULL,
    type                 TEXT                     NOT NULL,
    soknad_id            UUID                     NULL,
    journalpost_id       TEXT                     NULL,
    oppgave_id           UUID                     NULL
);
