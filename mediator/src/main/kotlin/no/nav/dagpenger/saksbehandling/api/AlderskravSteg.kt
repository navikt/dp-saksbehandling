package no.nav.dagpenger.saksbehandling.api

import no.nav.dagpenger.behandling.opplysninger.api.models.BehandlingDTO
import no.nav.dagpenger.behandling.opplysninger.api.models.UtledningDTO
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.api.models.OpplysningDTO
import no.nav.dagpenger.saksbehandling.api.models.OpplysningTypeDTO
import no.nav.dagpenger.saksbehandling.api.models.StegDTO
import no.nav.dagpenger.saksbehandling.api.models.SvarDTO

fun alderskravStegFra(behandlingDTO: BehandlingDTO?): StegDTO? {
    val alderskravOpplysning = alderskravOpplysningFra(behandlingDTO)
    return when {
        alderskravOpplysning != null -> {
            StegDTO(
                uuid = UUIDv7.ny(),
                stegNavn = "Under 67 år",
                opplysninger =
                    hentAlleOpplysninger(alderskravOpplysning).map {
                        OpplysningDTO(
                            opplysningNavn = it.opplysningstype,
                            opplysningType =
                                when (it.datatype) {
                                    "boolean" -> OpplysningTypeDTO.Boolean
                                    "string" -> OpplysningTypeDTO.String
                                    "double" -> OpplysningTypeDTO.Double
                                    "LocalDate" -> OpplysningTypeDTO.LocalDate
                                    else -> OpplysningTypeDTO.String
                                },
                            svar = SvarDTO(it.verdi),
                        )
                    },
            )
        }

        else -> null
    }
}

fun hentAlleOpplysninger(
    opplysningDTO: no.nav.dagpenger.behandling.opplysninger.api.models.OpplysningDTO,
): List<no.nav.dagpenger.behandling.opplysninger.api.models.OpplysningDTO> {
    val allOpplysning = mutableListOf<no.nav.dagpenger.behandling.opplysninger.api.models.OpplysningDTO>()

    fun traverseOpplysning(opplysningList: List<no.nav.dagpenger.behandling.opplysninger.api.models.OpplysningDTO>) {
        for (opplysning in opplysningList) {
            allOpplysning.add(opplysning)
            opplysning.utledetAv?.opplysninger?.let { traverseOpplysning(it) }
        }
    }

    traverseOpplysning(listOf(opplysningDTO))
    return allOpplysning.toList()
}

private fun alderskravOpplysningFra(behandling: BehandlingDTO?) =
    behandling?.opplysning?.findLast { it.opplysningstype == "Oppfyller kravet til alder" }
