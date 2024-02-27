package no.nav.dagpenger.saksbehandling.api

import no.nav.dagpenger.behandling.opplysninger.api.models.BehandlingDTO
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.api.models.OpplysningDTO
import no.nav.dagpenger.saksbehandling.api.models.OpplysningTypeDTO
import no.nav.dagpenger.saksbehandling.api.models.StegDTO
import no.nav.dagpenger.saksbehandling.api.models.SvarDTO

const val MINSTEINNTEKT_OPPLYSNING_NAVN = "Oppfyller kravet til alder"

fun minsteinntektStegFra(behandlingDTO: BehandlingDTO?): StegDTO? {
    val minsteinntektOpplysningTre = minsteinntektOpplysningFra(behandlingDTO)

    return when {
        minsteinntektOpplysningTre != null ->
            StegDTO(
                uuid = UUIDv7.ny(),
                stegNavn = "Har minste arbeidsinntekt",
                opplysninger =
                    listOf(
                        OpplysningDTO(
                            opplysningNavn = "Minsteinntekt",
                            opplysningType = OpplysningTypeDTO.Boolean,
                            svar = SvarDTO(minsteinntektOpplysningTre.verdi),
                        ),
                    ) + opplysningsgrunnlagFor(minsteinntektOpplysningTre),
            )

        else -> null
    }
}

private fun minsteinntektOpplysningFra(behandling: BehandlingDTO?) =
    behandling?.opplysning?.findLast { it.opplysningstype == MINSTEINNTEKT_OPPLYSNING_NAVN }

private fun opplysningsgrunnlagFor(opplysning: no.nav.dagpenger.behandling.opplysninger.api.models.OpplysningDTO) =
    opplysning.utledetAv?.opplysninger?.map {
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
