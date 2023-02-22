# language: no
Egenskap: ny rettighet avslått

  Bakgrunn:
    Gitt en ny søknad
      | fødselsnummer | behandlingId |
      | 12345678901   | 1            |
    Og alle inngangsvilkår er "ikke oppfylt" med virkningsdato "14.12.2022"
    Og beslutter kvalitetssikrer

  Scenario: mottaker har ikke rett til dagpenger
    Så skal bruker ha 1 vedtak