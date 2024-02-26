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
        alderskravOpplysning != null ->
            StegDTO(
                uuid = UUIDv7.ny(),
                stegNavn = "Under 67 år",
                opplysninger =
                listOf(
                    OpplysningDTO(
                        opplysningNavn = "Under 67 år",
                        opplysningType = OpplysningTypeDTO.Boolean,
                        svar = SvarDTO(alderskravOpplysning.verdi),
                    ),
                ) + opplysningsgrunnlagFor(alderskravOpplysning),
            )

        else -> null
    }
}

private fun alderskravOpplysningFra(behandling: BehandlingDTO?) =
    behandling?.opplysning?.findLast { it.opplysningstype == "Oppfyller kravet til alder" }

private fun tull(utledningDTO: UtledningDTO?, opplysninger: MutableList<OpplysningDTO>) {
    if(utledningDTO == null) return
    
}

private fun opplysningsgrunnlagFor(opplysning: no.nav.dagpenger.behandling.opplysninger.api.models.OpplysningDTO): List<OpplysningDTO> {
    val opplysninger = mutableListOf<OpplysningDTO>()
    opplysning.utledetAv!!.opplysninger.forEach {

    }
    return opplysning.utledetAv?.opplysninger?.map {
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
    } ?: emptyList()
}
