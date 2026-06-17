UPDATE emneknagg_v1
SET    emneknagg = 'Ordinær'
WHERE  emneknagg = 'Innvilgelse ordinær';

UPDATE emneknagg_v1
SET    emneknagg = 'Permittert'
WHERE  emneknagg = 'Innvilgelse permittering';

UPDATE emneknagg_v1
SET    emneknagg = 'Permittert fisk'
WHERE  emneknagg = 'Innvilgelse permittering fisk';

UPDATE emneknagg_v1
SET    emneknagg = 'Konkurs'
WHERE  emneknagg = 'Innvilgelse etter konkurs';
