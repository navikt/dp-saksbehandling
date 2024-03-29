package no.nav.dagpenger.saksbehandling.api

import no.nav.dagpenger.behandling.opplysninger.api.models.OpplysningDTO
import no.nav.dagpenger.saksbehandling.DataType
import no.nav.dagpenger.saksbehandling.Opplysning
import no.nav.dagpenger.saksbehandling.OpplysningStatus
import java.util.UUID

fun hentAlleUnikeOpplysningerFra(opplysningstre: OpplysningDTO): List<Opplysning> {
    val aggregerteOpplysninger = mutableMapOf<UUID, OpplysningDTO>()
    traverserOpplysningstre(
        opplysninger = listOf(opplysningstre),
        aggregerteOpplysninger = aggregerteOpplysninger,
    )
    return aggregerteOpplysninger.entries.map { it.value.tilOpplysning() }
}

private fun traverserOpplysningstre(
    opplysninger: List<OpplysningDTO>,
    aggregerteOpplysninger: MutableMap<UUID, OpplysningDTO>,
) {
    opplysninger.forEach { opplysningDTO ->
        aggregerteOpplysninger[opplysningDTO.id] = opplysningDTO
        opplysningDTO.utledetAv?.opplysninger?.let { traverserOpplysningstre(it, aggregerteOpplysninger) }
    }
}

private fun OpplysningDTO.tilOpplysning() =
    Opplysning(
        navn = this.opplysningstype,
        verdi = this.verdi,
        dataType = when (this.datatype) {
            "boolean" -> DataType.Boolean
            "LocalDate" -> DataType.LocalDate
            "int" -> DataType.Int
            "double" -> DataType.Double
            else -> DataType.String
        },
        status = when (this.status) {
            OpplysningDTO.Status.Hypotese -> OpplysningStatus.Hypotese
            OpplysningDTO.Status.Faktum -> OpplysningStatus.Faktum
        },
        redigerbar = this.redigerbar,
    )
