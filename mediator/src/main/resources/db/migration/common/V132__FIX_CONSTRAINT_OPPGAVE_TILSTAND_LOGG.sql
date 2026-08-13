DROP INDEX IF EXISTS idx_1_avsluttende_tilstand_per_oppgave;

CREATE UNIQUE INDEX idx_1_avsluttende_tilstand_per_oppgave
    ON      oppgave_tilstand_logg_v1(oppgave_id, tilstand)
    WHERE   tilstand IN ('FERDIG_BEHANDLET', 'AVBRUTT', 'AVBRUTT_MASKINELT')
    AND     hendelse_type != 'KlageinstansVedtakHendelse'
;