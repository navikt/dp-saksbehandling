UPDATE oppgave_tilstand_logg_v1
SET    hendelse = hendelse - 'søknadId'
    || jsonb_build_object('behandletHendelseId', hendelse -> 'søknadId', 'behandletHendelseType', 'Søknad')
WHERE hendelse_type IN ( 'ForslagTilVedtakHendelse', 'VedtakFattetHendelse', 'BehandlingAvbruttHendelse' )
AND   hendelse -> 'søknadId' IS NOT NULL;
