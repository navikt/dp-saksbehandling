-- Feil som følge av en breaking change i dp-behandling:
-- Denne feilen, og feilen over kommer av at vi har endret et vilkår i dag litt over kl 10.
-- Søknader som kom inn før det tidspunktet,
-- men ikke var ferdig behandlet "mangler" dermed en opplysning vi er avhengig av videre.
-- Vi har nå laget en fiks for at det skal fungere for de resterende som ligger på "vent",
-- også skal vi jobbe for at dette ikke skal skje igjen.
UPDATE utsending_v1
SET tilstand = 'Avbrutt'
WHERE id = '01942b90-96c2-7076-955c-daed73d5120b'
  AND tilstand = 'VenterPåVedtak';
