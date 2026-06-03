ALTER TABLE IF EXISTS outbox
    RENAME TO kafka_utboks_v1;

ALTER INDEX IF EXISTS idx_outbox_pending RENAME TO idx_kafka_utboks_v1_pending;
