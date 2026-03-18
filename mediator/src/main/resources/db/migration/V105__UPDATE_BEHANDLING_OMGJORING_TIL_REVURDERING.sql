ALTER TABLE behandling_v1 DISABLE TRIGGER oppdater_endret_tidspunkt;

UPDATE behandling_v1
SET    utlost_av = 'REVURDERING'
WHERE  utlost_av = 'OMGJØRING';

UPDATE oppgave_tilstand_logg_v1
SET    hendelse_type = 'RevurderingBehandlingOpprettetHendelse'
WHERE  hendelse_type = 'OmgjøringBehandlingOpprettetHendelse';

UPDATE hendelse_v1
SET    hendelse_type = 'RevurderingBehandlingOpprettetHendelse'
WHERE  hendelse_type = 'OmgjøringBehandlingOpprettetHendelse';

ALTER TABLE behandling_v1 ENABLE TRIGGER oppdater_endret_tidspunkt;
