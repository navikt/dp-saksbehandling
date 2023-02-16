# language: no
Egenskap: ny rettighet

  Scenario: mottaker har rett til dagpenger
    Gitt en ny søknad
      | fødselsnummer | behandlingId |
      | 12345678901   | 1            |
    Og alle inngangsvilkår er oppfylt
    Og sats er 488, grunnlag er 100000 og stønadsperiode er 52
    Og beslutter kvalitetssikrer
    Så skal bruker ha 1 vedtak
    Når rapporteringshendelse mottas
      | dato       | fravær |
      | 15.12.2022 | true   |
      | 16.12.2022 | false  |
      | 17.12.2022 | false  |
      | 18.12.2022 | false  |
    Så skal forbruket være 3
    Så skal bruker ha 2 vedtak