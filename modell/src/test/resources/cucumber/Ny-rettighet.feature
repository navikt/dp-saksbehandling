# language: no
Egenskap: ny rettighet

  Scenario: mottaker har rett til dagpenger
    Gitt en ny søknad
      | fødselsnummer | behandlingId |
      | 12345678901   | 1            |
    Og alle inngangsvilkår er "oppfylt" med virkningstidpunkt "14.12.2022"
    Og sats er 488, grunnlag er 100000 og stønadsperiode er 52
    Og beslutter kvalitetssikrer
    Så skal bruker ha 1 vedtak
    Når rapporteringshendelse mottas
      | dato       | fravær |
      | 14.12.2022 | false  |
      | 15.12.2022 | true   |
      | 16.12.2022 | false  |
      | 17.12.2022 | false  |
    Så skal forbruket være 2
    Så skal bruker ha 2 vedtak


  Scenario: mottaker har ikke rett til dagpenger
    Gitt en ny søknad
      | fødselsnummer | behandlingId |
      | 12345678901   | 1            |
    Og alle inngangsvilkår er "ikke oppfylt" med virkningstidpunkt "14.02.2023"
    Og beslutter kvalitetssikrer
    Så skal bruker ha 1 vedtak


  Scenario: mottaker har rett til dagpenger men sender meldekort for tidlig
    Gitt en ny søknad
      | fødselsnummer | behandlingId |
      | 12345678901   | 1            |
    Og alle inngangsvilkår er "oppfylt" med virkningstidpunkt "14.02.2023"
    Og sats er 488, grunnlag er 100000 og stønadsperiode er 52
    Og beslutter kvalitetssikrer
    Så skal bruker ha 1 vedtak
    Når rapporteringshendelse mottas
      | dato       | fravær |
      | 01.02.2023 | false  |
      | 02.02.2023 | true   |
      | 03.02.2023 | false  |
      | 04.02.2023 | false  |
    Så skal forbruket være 0
    Så skal bruker ha 2 vedtak

Egenskap: Meldekortberegning

  @wip
  Scenario: Beregning av første meldekort uten arbeidstimer og fravær
    Gitt at virkningsdatoen til vedtaket er fredag "03.02.2023", altså midt i meldeperioden
    Og vilkår for dagpenger er oppfylt i perioden
    Og vedtaket har 3 ventedager
    Når rapporteringshendelse mottas
      | dato       | fravær | timer |
      | 30.01.2023 | false  | 0     |
      | 31.01.2023 | false  | 0     |
      | 01.02.2023 | false  | 0     |
      | 02.02.2023 | false  | 0     |
      | 03.02.2023 | false  | 0     |
      | 04.02.2023 | false  | 0     |
      | 05.02.2023 | false  | 0     |
      | 06.02.2023 | false  | 0     |
      | 07.02.2023 | false  | 0     |
      | 08.02.2023 | false  | 0     |
      | 09.02.2023 | false  | 0     |
      | 10.02.2023 | false  | 0     |
      | 11.02.2023 | false  | 0     |
      | 12.02.2023 | false  | 0     |
    Så gjenstående ventedager være 0
    Så vil ventedager være avspasert på datoene "03.02.2023", "06.02.2023", "07.02.2023"
    Så vil gjenstående dagpengepengeperiode være redusert med 3 dager
    Så vil 3 dagsatser gå til utbetaling

  @wip
  Scenario: Beregning av første meldekort med arbeidstimer og uten fravær
    Gitt at virkningsdatoen til vedtaket er fredag "03.02.2023", altså midt i meldeperioden
    Og fastsatt arbeidstid er 40 t per uke, 8 t per dag
    Og vilkår for dagpenger er oppfylt i perioden
    Og vedtaket har 3 ventedager
    Når rapporteringshendelse mottas
      | dato       | fravær | timer |
      | 30.01.2023 | false  | 0     |
      | 31.01.2023 | false  | 0     |
      | 01.02.2023 | false  | 0     |
      | 02.02.2023 | false  | 0     |
      | 03.02.2023 | false  | 0     |
      | 04.02.2023 | false  | 0     |
      | 05.02.2023 | false  | 0     |
      | 06.02.2023 | false  | 8     |
      | 07.02.2023 | false  | 8     |
      | 08.02.2023 | false  | 0     |
      | 09.02.2023 | false  | 0     |
      | 10.02.2023 | false  | 8     |
      | 11.02.2023 | false  | 0     |
      | 12.02.2023 | false  | 0     |
    Så gjenstående ventedager være 0
    Så vil ventedager være avspasert på datoene "03.02.2023", "08.02.2023", "09.02.2023"
    Så vil gjenstående dagpengepengeperiode være redusert med 3 dager
    Så vil 3 dagsatser gå til utbetaling

  @wip
  Scenario: UNDER ARBEID, avspasering av prosenter av ventedager må avklares:
    Beregning av første meldekort med arbeidstimer og uten fravær
    Gitt at virkningsdatoen til vedtaket er fredag "03.02.2023", altså midt i meldeperioden
    Og fastsatt arbeidstid er 40 t per uke, 8 t per dag
    Og vilkår for dagpenger er oppfylt i perioden
    Og vedtaket har 3 ventedager
    Når rapporteringshendelse mottas
      | dato       | fravær | timer |
      | 30.01.2023 | false  | 0     |
      | 31.01.2023 | false  | 0     |
      | 01.02.2023 | false  | 0     |
      | 02.02.2023 | false  | 0     |
      | 03.02.2023 | false  | 4     |
      | 04.02.2023 | false  | 0     |
      | 05.02.2023 | false  | 0     |
      | 06.02.2023 | false  | 4.5   |
      | 07.02.2023 | false  | 8     |
      | 08.02.2023 | false  | 3.5   |
      | 09.02.2023 | false  | 4     |
      | 10.02.2023 | false  | 0     |
      | 11.02.2023 | false  | 0     |
      | 12.02.2023 | false  | 0     |
    Så gjenstående ventedager være 0
    Så vil ventedager være avspasert på datoene "03.02.2023", "08.02.2023", "09.02.2023"
    Så vil gjenstående dagpengepengeperiode være redusert med 3 dager
    Så vil 3 dagsatser gå til utbetaling
