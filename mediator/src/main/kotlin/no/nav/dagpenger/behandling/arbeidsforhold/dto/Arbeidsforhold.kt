package no.nav.dagpenger.behandling.arbeidsforhold.dto

data class Arbeidsforhold(
    val ansettelsesperiode: Ansettelsesperiode? = null,
    // Arbeidsavtaler - gjeldende og evt. med historikk
    val ansettelsesdetaljer: List<Ansettelsesdetaljer>? = null,
    // Arbeidsforhold-id fra opplysningspliktig
    val id: String? = null,
    val arbeidssted: Identer? = null,
    val arbeidstaker: Identer? = null,
    // Arbeidsforhold-id i AAREG
    val navArbeidsforholdId: Long? = null,
    val opplysningspliktig: Identer? = null,
    val permitteringer: List<Permittering>? = null,
    // Tidspunkt for siste bekreftelse av arbeidsforhold, format (ISO-8601): yyyy-MM-dd'T'HH:mm[:ss[.SSSSSSSSS]]
    val sistBekreftet: String? = null,
    // Arbeidsforholdtype (kodeverk: Arbeidsforholdtyper)
    val type: Kodeverksentitet? = null,
)
