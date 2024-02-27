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

const val ALDERSKRAV_OPPLYSNING_NAVN = "Oppfyller kravet til alder"

fun alderskravStegFra(behandlingDTO: BehandlingDTO?): StegDTO? {
    val alderskravOpplysningsTre = alderskravOpplysningFra(behandlingDTO)
    return when {
        alderskravOpplysningsTre != null ->
            StegDTO(
                uuid = UUIDv7.ny(),
                stegNavn = "Under 67 år",
                opplysninger = hentAlleOpplysningerFra(alderskravOpplysningsTre).tilOpplysningsDTOer(),
            )

        else -> null
    }
}

private fun alderskravOpplysningFra(behandling: BehandlingDTO?) =
    behandling?.opplysning?.findLast { it.opplysningstype == ALDERSKRAV_OPPLYSNING_NAVN }

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
