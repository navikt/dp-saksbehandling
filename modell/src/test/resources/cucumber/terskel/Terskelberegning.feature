# language: no

  Egenskap: Terskel

    Bakgrunn: Alt er fastsatt
      Gitt en ny søknad
        | fødselsnummer | behandlingId |
        | 12345678901   | 1            |
      Og alle inngangsvilkår er oppfylt med virkningsdato "14.12.2022" og fastsatt abreidstid er 8 timer
      Og sats er 488, grunnlag er 100000 og stønadsperiode er 52
      Og beslutter kvalitetssikrer
      Så skal bruker ha 1 vedtak

      Scenario: Rapporterer arbeidstimer eksakt lik terskel
        Når rapporteringshendelse mottas
          | dato       | fravær | timer |
          | 12.12.2022 | false  | 4     |
          | 13.12.2022 | false  | 4     |
          | 14.12.2022 | false  | 4     |
          | 15.12.2022 | false  | 4     |
          | 16.12.2022 | false  | 4     |
          | 17.12.2022 | false  | 0     |
          | 18.12.2022 | false  | 0     |
          | 19.12.2022 | false  | 4     |
          | 20.12.2022 | false  | 4     |
          | 21.12.2022 | false  | 4     |
          | 22.12.2022 | false  | 4     |
          | 23.12.2022 | false  | 4     |
          | 24.12.2022 | false  | 0     |
          | 25.12.2022 | false  | 0     |
        Så skal forbruket være 10 dager
        Så skal bruker ha 2 vedtak

      Scenario: Rapporterer arbeidstimer over terskel
        Når rapporteringshendelse mottas
          | dato       | fravær | timer |
          | 12.12.2022 | false  | 4     |
          | 13.12.2022 | false  | 4     |
          | 14.12.2022 | false  | 4     |
          | 15.12.2022 | false  | 4     |
          | 16.12.2022 | false  | 4     |
          | 17.12.2022 | false  | 0     |
          | 18.12.2022 | false  | 0     |
          | 19.12.2022 | false  | 4     |
          | 20.12.2022 | false  | 4     |
          | 21.12.2022 | false  | 4     |
          | 22.12.2022 | false  | 4     |
          | 23.12.2022 | false  | 4.5   |
          | 24.12.2022 | false  | 0     |
          | 25.12.2022 | false  | 0     |
          Så skal forbruket være 0 dager
          Så skal bruker ha 2 vedtak

      Scenario: Rapporterer fravær og arbeidstimer under terskel
        Når rapporteringshendelse mottas
          | dato       | fravær | timer |
          | 12.12.2022 | false  | 4     |
          | 13.12.2022 | false  | 4     |
          | 14.12.2022 | false  | 4     |
          | 15.12.2022 | false  | 4     |
          | 16.12.2022 | false  | 4     |
          | 17.12.2022 | false  | 0     |
          | 18.12.2022 | false  | 0     |
          | 19.12.2022 | false  | 4     |
          | 20.12.2022 | false  | 4     |
          | 21.12.2022 | false  | 4     |
          | 22.12.2022 | false  | 4     |
          | 23.12.2022 | true   | 0     |
          | 24.12.2022 | false  | 0     |
          | 25.12.2022 | false  | 0     |
        Så skal forbruket være 9 dager
        Så skal bruker ha 2 vedtak
