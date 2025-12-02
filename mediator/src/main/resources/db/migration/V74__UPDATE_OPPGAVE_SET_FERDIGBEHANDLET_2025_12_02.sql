UPDATE oppgave_v1
SET    tilstand = 'FERDIG_BEHANDLET'
WHERE  id = '019ad567-0e16-77c9-bea7-38446159c61a'
AND    tilstand = 'UNDER_BEHANDLING';

INSERT INTO oppgave_tilstand_logg_v1 (
    id,
    oppgave_id,
    tilstand,
    hendelse_type,
    hendelse,
    tidspunkt
) VALUES (
    gen_random_uuid()
    , '019ad567-0e16-77c9-bea7-38446159c61a'
    , 'FERDIG_BEHANDLET'
    , 'GodkjentBehandlingHendelse'
    , '{"oppgaveId": "019ad567-0e16-77c9-bea7-38446159c61a", "utførtAv": {"grupper": ["2e9c63d8-322e-4c1f-b500-a0abb812761c"], "navIdent": "A164486", "tilganger": ["SAKSBEHANDLER"]}, "meldingOmVedtakKilde": "DP_SAK"}'
    , now() + INTERVAL '1 hours'
);
