package no.nav.dagpenger.saksbehandling.api

import no.nav.dagpenger.behandling.opplysninger.api.models.BehandlingDTO
import no.nav.dagpenger.saksbehandling.Opplysning
import no.nav.dagpenger.saksbehandling.Steg

const val MINSTEINNTEKT_OPPLYSNING_NAVN = "Oppfyller kravet til alder"

fun minsteinntektStegFra(behandlingDTO: BehandlingDTO?): Steg? {
    val minsteinntektOpplysningTre = minsteinntektOpplysningFra(behandlingDTO)

    return when {
        minsteinntektOpplysningTre != null -> {
            Steg(
                navn = "Har minste arbeidsinntekt",
                opplysninger =
                    listOf(
                        Opplysning(
                            navn = "Minsteinntekt",
                            verdi = minsteinntektOpplysningTre.verdi,
                            dataType = "Boolean",
                        ),
                    ) + hentAlleOpplysningerFra(minsteinntektOpplysningTre),
            )
        }

        else -> null
    }
}

private fun minsteinntektOpplysningFra(behandling: BehandlingDTO?) =
    behandling?.opplysning?.findLast { it.opplysningstype == MINSTEINNTEKT_OPPLYSNING_NAVN }
