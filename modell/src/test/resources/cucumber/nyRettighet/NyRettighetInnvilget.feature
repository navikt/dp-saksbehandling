# language: no
Egenskap: Ny rettighet innvilget

  Bakgrunn: Innvilget vedtak fra 14.12.2022
    Gitt en ny søknad
      | fødselsnummer | behandlingId |
      | 12345678901   | 1            |
    Og alle inngangsvilkår er oppfylt med virkningsdato "14.12.2022" og fastsatt abreidstid er 8 timer
    Og sats er 488, grunnlag er 100000 og stønadsperiode er 52
    Og beslutter kvalitetssikrer
    Så skal bruker ha 1 vedtak

  Scenario: Mottar rapportering fra bruker som medfører forbruk av stønadsperiode og vedtak om utbetaling
    Når rapporteringshendelse mottas
      | dato       | fravær | timer |
      | 14.12.2022 | false  | 4.5   |
      | 15.12.2022 | true   | 5     |
      | 16.12.2022 | false  | 5     |
      | 17.12.2022 | false  | 5     |
    Så skal forbruket være 2
    Så skal bruker ha 2 vedtak

  Scenario: Mottar rapportering fra bruker for periode utenfor dagpengevedtaket
    Når rapporteringshendelse mottas
      | dato       | fravær | timer |
      | 10.12.2022 | false  | 4.5   |
      | 11.12.2022 | true   | 5     |
      | 12.12.2022 | false  | 5     |
      | 13.12.2022 | false  | 5     |
    Så skal forbruket være 0
    Så skal bruker ha 2 vedtak
