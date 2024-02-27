package no.nav.dagpenger.saksbehandling.api

import no.nav.dagpenger.behandling.opplysninger.api.models.OpplysningDTO
import no.nav.dagpenger.saksbehandling.Opplysning

fun hentAlleOpplysningerFra(opplysningsTre: OpplysningDTO): List<Opplysning> {
    val aggregerteOpplysninger = mutableListOf<OpplysningDTO>()
    traverserOpplysningsTre(
        opplysninger = listOf(opplysningsTre),
        aggregerteOpplysninger = aggregerteOpplysninger,
    )
    return aggregerteOpplysninger.map { it.toOpplysning() }
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

private fun OpplysningDTO.toOpplysning() =
    Opplysning(
        navn = this.opplysningstype,
        verdi = this.verdi,
        dataType = this.datatype,
    )
