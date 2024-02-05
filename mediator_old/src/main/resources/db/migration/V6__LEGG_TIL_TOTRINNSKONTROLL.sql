ALTER TABLE steg
    ADD COLUMN krever_totrinnskontroll BOOLEAN DEFAULT FALSE;

UPDATE steg
SET krever_totrinnskontroll = FALSE
WHERE krever_totrinnskontroll IS NULL;
