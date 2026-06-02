-- Pga timeout mot regelmotor har to oppgaver kommet ut av synk med regelmotor. En av oppgavene har fått sendt ut vedtaksbrev, den andre ikke.
DO $$
    DECLARE
        person RECORD;
    BEGIN
        FOR person IN
            SELECT  *
            FROM    person_v1 pers
            WHERE   pers.id IN('019b27b7-20e7-76e5-8f53-950eeccc92e8')
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
