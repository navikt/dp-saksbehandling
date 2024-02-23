package no.nav.dagpenger.saksbehandling.api

import no.nav.dagpenger.behandling.opplysninger.api.models.BehandlingDTO
import no.nav.dagpenger.saksbehandling.UUIDv7
import no.nav.dagpenger.saksbehandling.api.models.OpplysningDTO
import no.nav.dagpenger.saksbehandling.api.models.OpplysningTypeDTO
import no.nav.dagpenger.saksbehandling.api.models.StegDTO
import no.nav.dagpenger.saksbehandling.api.models.SvarDTO

fun minsteinntektSteg(behandling: BehandlingDTO?): StegDTO? {
    val minsteinntektOpplysning = minsteinntektOpplysningFra(behandling)

    return if (minsteinntektOpplysning != null) {
        StegDTO(
            uuid = UUIDv7.ny(),
            stegNavn = "Har minste arbeidsinntekt",
            opplysninger =
                listOf(
                    OpplysningDTO(
                        opplysningNavn = "Minsteinntekt",
                        opplysningType = OpplysningTypeDTO.Boolean,
                        svar = SvarDTO(minsteinntektOpplysning.verdi),
                    ),
                ) + opplysningsgrunnlagFor(minsteinntektOpplysning),
        )
    } else {
        null
    }
}

private fun minsteinntektOpplysningFra(behandling: BehandlingDTO?) =
    behandling?.opplysning?.findLast { it.opplysningstype == "Minsteinntekt" }

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
