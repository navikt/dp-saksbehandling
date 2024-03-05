package no.nav.dagpenger.saksbehandling.api

import no.nav.dagpenger.behandling.opplysninger.api.models.OpplysningDTO
import no.nav.dagpenger.saksbehandling.Opplysning
import no.nav.dagpenger.saksbehandling.OpplysningStatus
import java.util.UUID

fun hentAlleUnikeOpplysningerFra(opplysningstre: OpplysningDTO): List<Opplysning> {
    val aggregerteOpplysninger = mutableMapOf<UUID, OpplysningDTO>()
    traverserOpplysningstre(
        opplysninger = listOf(opplysningstre),
        aggregerteOpplysninger = aggregerteOpplysninger,
    )
    return aggregerteOpplysninger.entries.map { it.value.toOpplysning() }
}

private fun traverserOpplysningstre(
    opplysninger: List<OpplysningDTO>,
    aggregerteOpplysninger: MutableMap<UUID, OpplysningDTO>,
) {
    opplysninger.forEach { opplysning ->
        aggregerteOpplysninger[opplysning.id] = opplysning
        opplysning.utledetAv?.opplysninger?.let { traverserOpplysningstre(it, aggregerteOpplysninger) }
    }
}

private fun OpplysningDTO.toOpplysning() =
    Opplysning(
        navn = this.opplysningstype,
        verdi = this.verdi,
        dataType = this.datatype,
        status = when (this.status) {
            OpplysningDTO.Status.Hypotese -> OpplysningStatus.Hypotese
            OpplysningDTO.Status.Faktum -> OpplysningStatus.Faktum
        },
    )
