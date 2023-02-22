# language: no
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
    Så vil gjenstående ventedager være 0
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
    Så vil gjenstående dagpengepengeperiode være redusert med 0 dager
    Så vil 0 kr gå til utbetaling

  @wip
  Scenario: Ventedager avspaseres prosentvis pga deltidsarbeid under terskel
    Gitt at virkningsdatoen til vedtaket er fredag "03.02.2023", altså midt i meldeperioden
    Og dagsats er 800
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
      | 06.02.2023 | false  | 0     |
      | 07.02.2023 | false  | 4     |
      | 08.02.2023 | false  | 3     |
      | 09.02.2023 | false  | 0     |
      | 10.02.2023 | false  | 0     |
      | 11.02.2023 | false  | 0     |
      | 12.02.2023 | false  | 0     |
    Så vil ventiden avspaseres fullt ut
    Så vil ventedager avspaseres på datoene "03.02.2023", "06.02.2023", "07.02.2023", "08.02.2023"
    Så vil stønadsperiode reduseres med 3 dager
    Så vil 1700 kr gå til utbetaling

  @wip
  Scenario: Ventedager avspaseres ikke i løpet av de første 15 arbeidsdagene
    Gitt at virkningsdatoen til vedtaket er fredag "03.02.2023", altså midt i meldeperioden
    Og dagsats er 800
    Og fastsatt arbeidstid er 40 t per uke, 8 t per dag
    Og vilkår for dagpenger er oppfylt i perioden
    Og vedtaket har 3 ventedager
    Når rapporteringshendelse mottas
      | dato       | fravær | timer |
      | 30.01.2023 | false  | 0     |
      | 31.01.2023 | false  | 0     |
      | 01.02.2023 | false  | 0     |
      | 02.02.2023 | false  | 0     |
      | 03.02.2023 | true   | 0     |
      | 04.02.2023 | false  | 0     |
      | 05.02.2023 | false  | 0     |
      | 06.02.2023 | false  | 4     |
      | 07.02.2023 | false  | 4     |
      | 08.02.2023 | false  | 4     |
      | 09.02.2023 | false  | 4     |
      | 10.02.2023 | false  | 4     |
      | 11.02.2023 | false  | 0     |
      | 12.02.2023 | false  | 0     |
    Så vil ventiden reduseres med 2.5 dager
    Så vil ventedager avspaseres på datoene "06.02.2023", "07.02.2023", "08.02.2023", "09.02.2023", "10.02.2023"
    Så vil stønadsperiode reduseres med 0 dager
    Så vil 0 kr gå til utbetaling
    Når rapporteringshendelse mottas
      | dato       | fravær | timer |
      | 13.02.2023 | false  | 8     |
      | 14.02.2023 | false  | 8     |
      | 15.02.2023 | false  | 8     |
      | 16.02.2023 | false  | 8     |
      | 17.02.2023 | false  | 8     |
      | 18.02.2023 | false  | 0     |
      | 19.02.2023 | false  | 0     |
      | 20.02.2023 | false  | 8     |
      | 21.02.2023 | false  | 8     |
      | 22.02.2023 | false  | 8     |
      | 23.02.2023 | false  | 8     |
      | 24.02.2023 | false  | 8     |
      | 25.02.2023 | false  | 0     |
      | 26.02.2023 | false  | 0     |
    Så skal brukers vedtak avsluttes??? PS: I dag settes til-dato lik virkningsdato
