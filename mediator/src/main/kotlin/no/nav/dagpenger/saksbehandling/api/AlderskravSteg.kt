package no.nav.dagpenger.saksbehandling.api

import no.nav.dagpenger.behandling.opplysninger.api.models.BehandlingDTO
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.api.models.OpplysningDTO
import no.nav.dagpenger.saksbehandling.api.models.OpplysningTypeDTO.Boolean
import no.nav.dagpenger.saksbehandling.api.models.OpplysningTypeDTO.Double
import no.nav.dagpenger.saksbehandling.api.models.OpplysningTypeDTO.LocalDate
import no.nav.dagpenger.saksbehandling.api.models.OpplysningTypeDTO.String
import no.nav.dagpenger.saksbehandling.api.models.StegDTO
import no.nav.dagpenger.saksbehandling.api.models.SvarDTO
import no.nav.dagpenger.behandling.opplysninger.api.models.OpplysningDTO as BehandlingOpplysningDTO

fun alderskravStegFra(behandlingDTO: BehandlingDTO?): StegDTO? {
    val alderskravOpplysning = alderskravOpplysningFra(behandlingDTO)
    return when {
        alderskravOpplysning != null ->
            StegDTO(
                uuid = UUIDv7.ny(),
                stegNavn = "Under 67 år",
                opplysninger = hentAlleBehandlingsOpplysninger(alderskravOpplysning).tilOpplysningsDTOer(),
            )

        else -> null
    }
}

private fun Collection<BehandlingOpplysningDTO>.tilOpplysningsDTOer() = this.map { it.tilOpplysningDTO() }

private fun BehandlingOpplysningDTO.tilOpplysningDTO() =
    OpplysningDTO(
        opplysningNavn = this.opplysningstype,
        opplysningType =
            when (this.datatype) {
                "boolean" -> Boolean
                "string" -> String
                "double" -> Double
                "LocalDate" -> LocalDate
                else -> String
            },
        svar = SvarDTO(this.verdi),
    )

private fun hentAlleBehandlingsOpplysninger(opplysningDTO: BehandlingOpplysningDTO): List<BehandlingOpplysningDTO> {
    val aggregerteOpplysninger = mutableListOf<BehandlingOpplysningDTO>()
    traverserOpplysningsTre(
        opplysninger = listOf(opplysningDTO),
        aggregerteOpplysninger = aggregerteOpplysninger,
    )
    return aggregerteOpplysninger.toList()
}

private fun traverserOpplysningsTre(
    opplysninger: List<BehandlingOpplysningDTO>,
    aggregerteOpplysninger: MutableList<BehandlingOpplysningDTO>,
) {
    for (opplysning in opplysninger) {
        aggregerteOpplysninger.add(opplysning)
        opplysning.utledetAv?.opplysninger?.let { traverserOpplysningsTre(it, aggregerteOpplysninger) }
    }
}

private fun alderskravOpplysningFra(behandling: BehandlingDTO?) =
    behandling?.opplysning?.findLast { it.opplysningstype == "Oppfyller kravet til alder" }
