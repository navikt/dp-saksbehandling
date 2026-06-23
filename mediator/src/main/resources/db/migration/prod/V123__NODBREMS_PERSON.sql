DO $$
    DECLARE
        person RECORD;
    BEGIN
        FOR person IN
            SELECT  *
            FROM    person_v1 pers
            WHERE   pers.id IN('019e3cc8-4953-77ef-be65-69df0a5276df')
            LOOP
                INSERT INTO nodbremset_person_v1
                ( person_id
                )
                VALUES
                    ( person.id
                    )
                ON CONFLICT DO NOTHING ;
            END LOOP;
    END $$;
