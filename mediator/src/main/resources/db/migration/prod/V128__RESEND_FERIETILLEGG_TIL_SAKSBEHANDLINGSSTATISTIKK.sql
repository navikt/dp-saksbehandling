INSERT
INTO saksbehandling_statistikk_v1
        ( tilstand_id
        , tilstand
        , tilstand_tidspunkt
        , oppgave_id
        , mottatt
        , sak_id
        , behandling_id
        , person_ident
        , saksbehandler_ident
        , beslutter_ident
        , utlost_av
        , behandling_resultat
        , fagsystem
        , behandling_aarsak
        , arena_sak_id
        , resultat_begrunnelse )
SELECT
          tilstand_id
        , tilstand
        , tilstand_tidspunkt
        , oppgave_id
        , mottatt
        , sak_id
        , behandling_id
        , person_ident
        , saksbehandler_ident
        , beslutter_ident
        , utlost_av
        , behandling_resultat
        , fagsystem
        , behandling_aarsak
        , arena_sak_id
        , resultat_begrunnelse
FROM    saksbehandling_statistikk_v1 sast
WHERE   sast.tilstand = 'FERDIG_BEHANDLET'
AND     sast.behandling_id IN(
        '019eb19c-5f3c-704d-ad1f-7fcd662c3974',
        '019eb19c-60fb-7406-927b-9974e41469b2',
        '019eb19c-5d09-7219-8095-dac4e4bd5ea0',
        '019eb19c-6283-7fac-a0d1-3ec8144fa041',
        '019eb19c-6624-7a34-82bd-38a652a7e806',
        '019eb19c-677b-7e99-96fb-2a142bb83f16',
        '019eb19c-68fd-79a1-b1e8-f75a1d9045a0',
        '019eb19c-6a55-7e33-bd32-f7a4872f5b65',
        '019eb19c-6b56-779f-9370-964a45526712',
        '019eb19c-6df8-77d1-8f5c-290fbda2589c',
        '019eb19c-6f44-7356-9e5d-c37adafb9a15',
        '019eb19c-5cf7-7f46-8e49-e519afd75e28',
        '019eb19c-7287-728e-9bb5-5cc7ad0080b2',
        '019eb19c-745d-7a63-b484-75cd1e0e7351',
        '019eb19c-7655-7049-a92b-17f37fe595a9',
        '019eb19c-77d5-7b1c-a34d-d7a4c0b643c7',
        '019eb19c-79b9-709e-92d5-2900ad1cd714',
        '019eb19c-7b3b-736a-a91a-5b9085df7200',
        '019eb19c-7cfa-73ba-84ed-9799ef10e4de',
        '019eb19c-7e74-75c5-8fb0-7f0003764855',
        '019eb19c-801f-791b-b824-16ed7af806ab',
        '019eb19c-5f8a-7102-8409-79207e96c13f',
        '019eb19c-613a-7b5f-ab17-cbff68a8f5b6',
        '019eb19c-6314-73e1-ac06-5c120a2d40a8',
        '019eb19c-64b2-72e5-b542-e25d951710c5',
        '019eb19c-6715-77d3-8cd4-3d2578d1b553',
        '019eb19c-68b5-794c-81de-a1031539878f',
        '019eb19c-6bf0-75d7-9f67-b12d25dae583',
        '019eb19c-6eb5-75f4-9b85-538b01de8020',
        '019eb19c-70d2-7b7e-8c51-7aa2272f89ad',
        '019eb19c-720e-75f0-a723-677553561f82',
        '019eb19c-73d0-7d45-a37c-e5cb71f7381b',
        '019eb19c-7528-7645-a09b-7125e10abf0b',
        '019eb19c-76c7-7708-a170-e400665e020c',
        '019eb19c-786c-7a60-99bf-0df78c3a5bd4',
        '019eb19c-7c77-751a-a3b4-5b3c9cd6a559',
        '019eb19c-7da0-7d90-ada0-0f0c40817254',
        '019eb19c-7f45-7aed-ae06-5806b08d17aa',
        '019eb19c-80c8-7db5-b6ef-efb57a75cec8',
        '019eb19c-82fe-7115-af3e-af106df62910',
        '019eb19c-854b-7803-a6d9-31bc2e0fe208',
        '019eb19c-87ad-7672-ba54-61ada05299ba',
        '019eb19c-894b-7596-90d1-a078b751fca1',
        '019eb19c-8b0c-77e0-9657-b5df894f9128',
        '019eb19c-8e08-7167-95e0-e806a9a1d3ab'
    );
