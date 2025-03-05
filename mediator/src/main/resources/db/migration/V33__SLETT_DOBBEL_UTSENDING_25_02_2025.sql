-- Feil som følge av duplikate utsendinger pga ingen partisjonsnøkkel i dp-behov-pdf-generator
-- Se https://favro.com/widget/98c34fb974ce445eac854de0/64bb80fb668e87b8f3440cfc?card=NAV-24460 for detaljer
DELETE
FROM utsending_v1
WHERE id = '01953cc1-8600-7e12-9242-1e0d4d6824d3'
  AND tilstand = 'VenterPåVedtak';