package no.nav.dagpenger.saksbehandling

class AlderskravSteg2(beskrivendeId: String, opplysninger: List<Opplysning>) : VilkårSteg(
    beskrivendeId = beskrivendeId,
    opplysninger = opplysninger,
) {

    override val rotNodeNavn: String = "Oppfyller kravet til alder"
}
