-- Rydder i gamle oppgaver som ikke har blitt oppdatert riktig i systemet.
-- Disse er behandlet i Arena.
UPDATE oppgave_v1
SET    tilstand = 'BEHANDLES_I_ARENA'
WHERE  tilstand = 'OPPRETTET';
