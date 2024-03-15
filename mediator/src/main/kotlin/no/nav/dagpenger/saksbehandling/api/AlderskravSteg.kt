package no.nav.dagpenger.saksbehandling.api

import no.nav.dagpenger.behandling.opplysninger.api.models.BehandlingDTO
import no.nav.dagpenger.behandling.opplysninger.api.models.OpplysningDTO
import no.nav.dagpenger.saksbehandling.AlderskravSteg
import no.nav.dagpenger.saksbehandling.Steg

const val ALDERSKRAV_OPPLYSNING_NAVN = "Oppfyller kravet til alder"
val alderBeskrivendeId = "steg.alder"

fun alderskravStegFra(behandlingDTO: BehandlingDTO?): Steg? {
    val alderskravOpplysningsTre: OpplysningDTO? = alderskravOpplysningFra(behandlingDTO)
    return when {
        alderskravOpplysningsTre != null ->
            AlderskravSteg(
                opplysninger = hentAlleUnikeOpplysningerFra(alderskravOpplysningsTre),
            )

        else -> null
    }
}

private fun alderskravOpplysningFra(behandling: BehandlingDTO?) =
    behandling?.opplysning?.findLast { it.opplysningstype == ALDERSKRAV_OPPLYSNING_NAVN }
