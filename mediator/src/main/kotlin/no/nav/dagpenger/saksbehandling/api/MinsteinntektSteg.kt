package no.nav.dagpenger.saksbehandling.api

import no.nav.dagpenger.behandling.opplysninger.api.models.BehandlingDTO
import no.nav.dagpenger.saksbehandling.Steg

const val MINSTEINNTEKT_OPPLYSNING_NAVN = "Minsteinntekt"

fun minsteinntektStegFra(behandlingDTO: BehandlingDTO?): Steg? {
    val minsteinntektOpplysningTre = minsteinntektOpplysningFra(behandlingDTO)

    return when {
        minsteinntektOpplysningTre != null -> Steg(
            navn = "Har minste arbeidsinntekt",
            opplysninger = hentAlleOpplysningerFra(minsteinntektOpplysningTre),
        )

        else -> null
    }
}

private fun minsteinntektOpplysningFra(behandling: BehandlingDTO?) =
    behandling?.opplysning?.findLast { it.opplysningstype == MINSTEINNTEKT_OPPLYSNING_NAVN }
