package no.nav.dagpenger.saksbehandling.api

import no.nav.dagpenger.behandling.opplysninger.api.models.OpplysningDTO

fun hentAlleOpplysningerFra(opplysningsTre: OpplysningDTO): List<OpplysningDTO> {
    val aggregerteOpplysninger = mutableListOf<OpplysningDTO>()
    traverserOpplysningsTre(
        opplysninger = listOf(opplysningsTre),
        aggregerteOpplysninger = aggregerteOpplysninger,
    )
    return aggregerteOpplysninger.toList()
}

private fun traverserOpplysningsTre(
    opplysninger: List<OpplysningDTO>,
    aggregerteOpplysninger: MutableList<OpplysningDTO>,
) {
    for (opplysning in opplysninger) {
        aggregerteOpplysninger.add(opplysning)
        opplysning.utledetAv?.opplysninger?.let { traverserOpplysningsTre(it, aggregerteOpplysninger) }
    }
}
