UPDATE  utsending_v1 AS utse
SET     type = 'KLAGE_OVERSENDELSE'
WHERE   type = 'KLAGEMELDING'
AND     EXISTS (
        SELECT 1
        FROM  klage_v1 AS klag
            , jsonb_array_elements(klag.opplysninger) AS oppl
        WHERE klag.id = utse.behandling_id
        AND oppl ->> 'type' = 'UTFALL'
        AND oppl ->  'verdi' ->> 'value' = 'Opprettholdelse'
);
UPDATE  utsending_v1 AS utse
SET     type = 'KLAGE_AVVIST'
WHERE   type = 'KLAGEMELDING'
AND     EXISTS (
        SELECT 1
        FROM  klage_v1 AS klag
            , jsonb_array_elements(klag.opplysninger) AS oppl
        WHERE klag.id = utse.behandling_id
        AND oppl ->> 'type' = 'UTFALL'
        AND oppl ->  'verdi' ->> 'value' = 'Avvist'
);
