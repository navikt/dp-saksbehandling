package no.nav.dagpenger.saksbehandling

class AlderskravSteg(opplysninger: List<Opplysning>) : VilkårSteg(
    beskrivendeId = ALDER_BESKRIVENDE_ID,
    opplysninger = opplysninger,
    toppnodeNavn = ALDERSKRAV_OPPLYSNING_NAVN,
) {
    companion object {
        const val ALDERSKRAV_OPPLYSNING_NAVN = "Oppfyller kravet til alder"
        const val ALDER_BESKRIVENDE_ID = "steg.alder"
    }
}
