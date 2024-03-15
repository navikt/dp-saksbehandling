package no.nav.dagpenger.saksbehandling

class AlderskravSteg(beskrivendeId: String, opplysninger: List<Opplysning>) : VilkårSteg(
    beskrivendeId = beskrivendeId,
    opplysninger = opplysninger,
    toppNodeNavn = ALDERSKRAV_OPPLYSNING_NAVN,
) {
    companion object {
        const val ALDERSKRAV_OPPLYSNING_NAVN = "Oppfyller kravet til alder"
    }
}
