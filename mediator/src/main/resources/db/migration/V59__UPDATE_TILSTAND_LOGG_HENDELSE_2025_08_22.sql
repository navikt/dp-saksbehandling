UPDATE oppgave_tilstand_logg_v1
SET    hendelse = hendelse - 'søknadId'
    || jsonb_build_object('behandletHendelseId', hendelse -> 'søknadId', 'behandletHendelseType', 'Søknad')
WHERE id = '0198602d-7aa5-7543-bc7a-04ace14f147b';
