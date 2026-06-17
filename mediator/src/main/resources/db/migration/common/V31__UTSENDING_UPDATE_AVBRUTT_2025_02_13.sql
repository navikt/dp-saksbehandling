-- Feil som følge av at bruker kun er registrert som arbeidssøker til en dato før vedtaksdato.
-- Sakene er blitt vedtatt i Arena
UPDATE utsending_v1
SET tilstand = 'Avbrutt'
WHERE id in ('0194fa44-2cf3-75b1-971b-b0e942aa00e3', '0194f987-d4b5-7738-9a0f-9dc3abac9ab5')
  AND tilstand = 'VenterPåVedtak';
