--Migrerer eldre hendelser til nytt format, siden utførtAv nå er et objekt, ikke en string.
UPDATE hendelse_v1
SET    hendelse_data = hendelse_data - 'utførtAv' || jsonb_build_object('utførtAv', jsonb_build_object('navn', 'dp-behandling'))
WHERE  hendelse_type = 'SøknadsbehandlingOpprettetHendelse'
AND    hendelse_data ->> 'utførtAv' = 'dp-behandling'
;
