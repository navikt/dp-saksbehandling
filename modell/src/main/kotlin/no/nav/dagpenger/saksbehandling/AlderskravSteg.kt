package no.nav.dagpenger.saksbehandling

class AlderskravSteg(beskrivendeId: String, opplysninger: List<Opplysning>) : VilkårSteg(
    beskrivendeId = beskrivendeId,
    opplysninger = opplysninger,
) {

    override val toppNodeNavn: String = "Oppfyller kravet til alder"
}
