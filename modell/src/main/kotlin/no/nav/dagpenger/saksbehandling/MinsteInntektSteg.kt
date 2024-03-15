package no.nav.dagpenger.saksbehandling

class MinsteInntektSteg(opplysninger: List<Opplysning>) : VilkårSteg(
    beskrivendeId = MINSTEINNTEKT_BESKRIVENDE_ID,
    opplysninger = opplysninger,
    toppnodeNavn = MINSTEINNTEKT_OPPLYSNING_NAVN,
) {
    companion object {
        const val MINSTEINNTEKT_OPPLYSNING_NAVN = "Krav til minsteinntekt"
        const val MINSTEINNTEKT_BESKRIVENDE_ID = "steg.minsteinntekt"
    }
}
