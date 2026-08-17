-- KLAGEN_GJELDER_VEDTAK ble endret fra Datatype.TEKST til Datatype.UUID.
-- Eksisterende klager som fikk svar på dette feltet før endringen har verdien
-- lagret med datatype "TekstVerdi", selv om innholdet allerede er en gyldig UUID
-- (siden feltet alltid har blitt fylt inn med en behandlingId/vedtakId).
--
-- Uten denne migreringen feiler Opplysning sin init-validering ved lasting av
-- klagen: "Opplysning av type UUID kan ikke ha verdi av type TekstVerdi".
--
-- Verifisert i prod: alle forekomster av KLAGEN_GJELDER_VEDTAK med datatype
-- TekstVerdi inneholder en gyldig UUID-streng, så det er trygt å bare endre
-- datatype-taggen uten å røre selve verdien.

UPDATE klage_v1
SET opplysninger = (
    SELECT jsonb_agg(
        CASE
            WHEN elem ->> 'type' = 'KLAGEN_GJELDER_VEDTAK'
                AND elem -> 'verdi' ->> 'datatype' = 'TekstVerdi'
                THEN jsonb_set(elem, '{verdi,datatype}', '"UUID"')
            ELSE elem
        END
    )
    FROM jsonb_array_elements(opplysninger) AS elem
)
WHERE opplysninger @> '[{"type": "KLAGEN_GJELDER_VEDTAK", "verdi": {"datatype": "TekstVerdi"}}]';
