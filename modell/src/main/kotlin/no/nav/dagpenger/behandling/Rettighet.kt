package no.nav.dagpenger.behandling.entitet

import java.time.LocalDate

class Rettighet(rettighetstype: Rettighetstype, utfall: Boolean, fomDato: LocalDate, tomDato: LocalDate? = null)

enum class Rettighetstype {
    OrdinæreDagpenger,
    DagpengerUnderPermittering,
    DagpengerUnderPermitteringFraFiskeindustrien,
    ForskutterteLønnsgarantimidler,
    UtdanningIKombinasjonMedDagpenger,
    EtableringAvEgenVirksomhetIKombinasjonMedDagpenger,
    EksportAvDagpengerTilEUEllerEØSLand,
    LokalArbeidssøking,
    DeltidArbeidssøking,
    RedusertArbeidssøkingVedUtdanning,
}
