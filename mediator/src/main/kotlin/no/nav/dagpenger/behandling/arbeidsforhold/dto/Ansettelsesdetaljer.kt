package no.nav.dagpenger.behandling.arbeidsforhold.dto

data class Ansettelsesdetaljer(
    val ansettelsesform: Kodeverksentitet? = null,
    // Antall timer per uke
    val antallTimerPrUke: Double? = null,
    // Arbeidstidsordning (kodeverk: Arbeidstidsordninger)
    val arbeidstidsordning: Kodeverksentitet? = null,
    val rapporteringsmaaneder: Rapporteringsmaaneder? = null,
    val sisteLoennsendring: String? = null,
    val sisteStillingsprosentendring: String? = null,
    // Stillingsprosent
    val avtaltStillingsprosent: Double? = null,
    // Yrke (kodeverk: Yrker)
    val yrke: Kodeverksentitet? = null,
    val fartsomraade: Kodeverksentitet? = null,
    val skipsregister: Kodeverksentitet? = null,
    val fartoeystype: Kodeverksentitet? = null,
)
