-- Feil ved fatting av vedtak i Arena. Oppgaven er tatt i Arena.
UPDATE utsending_v1
SET tilstand = 'Avbrutt'
WHERE id in (
             '0197c003-c759-79a4-ac12-58ed31c1215b',
             '0197c039-1ac1-7e6d-9b77-4f11d4b0fdba',
             '0197c043-5f5a-7a32-909f-edf407871612',
             '0197c075-4161-7c21-969a-3a3cb4679855',
             '0197c07a-9db7-779a-8c7b-665bae15723b',
             '0197c07e-bcda-7d52-b255-793111a4be8c')
  AND tilstand = 'VenterPåVedtak';
