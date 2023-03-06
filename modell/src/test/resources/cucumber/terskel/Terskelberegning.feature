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

      Scenario: Rapporterer arbeidstimer
        Når rapporteringshendelse mottas
          | dato       | fravær | timer |
          | 14.12.2022 | false  | 4     |
          | 15.12.2022 | false  | 4     |
          | 16.12.2022 | false  | 4     |
          | 17.12.2022 | false  | 4     |
          | 18.12.2022 | false  | 4     |
          | 19.12.2022 | false  | 0     |
          | 20.12.2022 | false  | 0     |
          | 21.12.2022 | false  | 4     |
          | 22.12.2022 | false  | 4     |
          | 23.12.2022 | false  | 4     |
          | 24.12.2022 | false  | 4     |
          | 25.12.2022 | false  | 4     |
          | 26.12.2022 | false  | 0     |
          | 27.12.2022 | false  | 0     |
        Så skal forbruket være 10
        Så skal bruker ha 2 vedtak

      Scenario: Rapporterer arbeidstimer
        Når rapporteringshendelse mottas
          | dato       | fravær | timer |
          | 28.12.2022 | false  | 8     |
          | 29.12.2022 | false  | 8     |
          | 30.12.2022 | false  | 8     |
          | 31.12.2022 | false  | 8     |
          | 01.01.2023 | false  | 8     |
          | 02.01.2023 | false  | 0     |
          | 03.01.2023 | false  | 0     |
          | 04.01.2023 | false  | 4     |
          | 05.01.2023 | false  | 4     |
          | 06.01.2023 | false  | 4     |
          | 07.01.2023 | false  | 4     |
          | 08.01.2023 | false  | 4     |
          | 09.01.2023 | false  | 0     |
          | 10.01.2023 | false  | 0     |
        Så skal forbruket være 0
        Så skal bruker ha 3 vedtak