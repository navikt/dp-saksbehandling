UPDATE  oppgave_tilstand_logg_v1
SET     hendelse_type = 'SkriptHendelse'
      , hendelse    = '{"utførtAv": {"navn": "V106__AVBRYT_OPPGAVE_OG_UTSENDING_2026_04_07.sql"}}'
WHERE   oppgave_id  = '019d25ce-34db-7229-beb6-aab34ce6e6f1'
AND     tilstand    = 'AVBRUTT';
