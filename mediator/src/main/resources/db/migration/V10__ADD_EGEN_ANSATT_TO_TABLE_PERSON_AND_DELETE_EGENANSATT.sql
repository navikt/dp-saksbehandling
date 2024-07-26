ALTER TABLE person_v1
ADD COLUMN skjermes_som_egne_ansatte BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE person_v1
SET    skjermes_som_egne_ansatte = egenansatt
WHERE  skjermes_som_egne_ansatte <> egenansatt;

ALTER TABLE person_v1
DROP COLUMN egenansatt;
