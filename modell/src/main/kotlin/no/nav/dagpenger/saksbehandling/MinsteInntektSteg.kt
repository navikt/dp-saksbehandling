package no.nav.dagpenger.saksbehandling

class MinsteInntektSteg(beskrivendeId: String, opplysninger: List<Opplysning>) : VilkårSteg(
    beskrivendeId = beskrivendeId,
    opplysninger = opplysninger,
    toppNodeNavn = MINSTEINNTEKT_OPPLYSNING_NAVN,
) {
    companion object {
        const val MINSTEINNTEKT_OPPLYSNING_NAVN = "Krav til minsteinntekt"
    }
}
