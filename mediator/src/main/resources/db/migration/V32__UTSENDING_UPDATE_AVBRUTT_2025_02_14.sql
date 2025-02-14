-- Feil som følge av at bruker kun er registrert som arbeidssøker til en dato før vedtaksdato.
-- Sakene er blitt vedtatt i Arena
UPDATE utsending_v1
SET tilstand = 'Avbrutt'
WHERE id in ('019503a9-0007-7ad3-ba83-30a0b4db9a41', '019503ac-b056-7503-9c0f-935efb38d67e')
  AND tilstand = 'VenterPåVedtak';
