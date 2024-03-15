package no.nav.dagpenger.saksbehandling

class MinsteInntektSteg(beskrivendeId: String, opplysninger: List<Opplysning>) : VilkårSteg(
    beskrivendeId = beskrivendeId,
    opplysninger = opplysninger,
) {
    companion object {
        const val MINSTEINNTEKT_OPPLYSNING_NAVN = "Krav til minsteinntekt"
    }

    override val toppNodeNavn: String = MINSTEINNTEKT_OPPLYSNING_NAVN
}
