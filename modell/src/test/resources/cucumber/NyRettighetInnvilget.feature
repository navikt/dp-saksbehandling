# language: no
Egenskap: ny rettighet innvilget

  Bakgrunn: Innvilget vedtak fra 14.12.2022
    Gitt en ny søknad
      | fødselsnummer | behandlingId |
      | 12345678901   | 1            |
    Og alle inngangsvilkår er "oppfylt" med virkningsdato "14.12.2022"
    Og sats er 488, grunnlag er 100000 og stønadsperiode er 52
    Og beslutter kvalitetssikrer

  Scenario: mottaker har rett til dagpenger
    Så skal bruker ha 1 vedtak
    Når rapporteringshendelse mottas
      | dato       | fravær |
      | 14.12.2022 | false  |
      | 15.12.2022 | true   |
      | 16.12.2022 | false  |
      | 17.12.2022 | false  |
    Så skal forbruket være 2
    Så skal bruker ha 2 vedtak

  Scenario: mottaker har rett til dagpenger men sender meldekort for tidlig
    Så skal bruker ha 1 vedtak
    Når rapporteringshendelse mottas
      | dato       | fravær |
      | 01.02.2022 | false  |
      | 02.02.2022 | true   |
      | 03.02.2022 | false  |
      | 04.02.2022 | false  |
    Så skal forbruket være 0
    Så skal bruker ha 2 vedtak
