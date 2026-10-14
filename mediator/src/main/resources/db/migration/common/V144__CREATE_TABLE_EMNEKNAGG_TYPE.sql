DROP TABLE IF EXISTS emneknagg_type_v1;

CREATE TABLE emneknagg_type_v1
(
    emneknagg            TEXT PRIMARY KEY,
    registrert_tidspunkt TIMESTAMP WITHOUT TIME ZONE DEFAULT timezone('Europe/Oslo'::text, current_timestamp)
);


INSERT INTO emneknagg_type_v1 (emneknagg)
SELECT DISTINCT emneknagg
FROM emneknagg_v1
WHERE emneknagg NOT LIKE 'Ettersending%';


