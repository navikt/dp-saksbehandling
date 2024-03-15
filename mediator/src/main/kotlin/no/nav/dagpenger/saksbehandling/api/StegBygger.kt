package no.nav.dagpenger.saksbehandling.api

import no.nav.dagpenger.behandling.opplysninger.api.models.BehandlingDTO
import no.nav.dagpenger.behandling.opplysninger.api.models.OpplysningDTO
import no.nav.dagpenger.saksbehandling.AlderskravSteg
import no.nav.dagpenger.saksbehandling.AlderskravSteg.Companion.ALDERSKRAV_OPPLYSNING_NAVN
import no.nav.dagpenger.saksbehandling.MinsteInntektSteg
import no.nav.dagpenger.saksbehandling.MinsteInntektSteg.Companion.MINSTEINNTEKT_OPPLYSNING_NAVN
import no.nav.dagpenger.saksbehandling.Steg
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

fun minsteinntektStegFra(behandlingDTO: BehandlingDTO?): Steg? {
    val minsteinntektOpplysningTre = minsteinntektOpplysningFra(behandlingDTO)

    return when {
        minsteinntektOpplysningTre != null ->
            MinsteInntektSteg(
                opplysninger = hentAlleUnikeOpplysningerFra(minsteinntektOpplysningTre),
            )

        else -> null
    }
}
private fun minsteinntektOpplysningFra(behandling: BehandlingDTO?) =
    behandling.opplysningTreFra(MINSTEINNTEKT_OPPLYSNING_NAVN)

private fun alderskravOpplysningFra(behandling: BehandlingDTO?) =
    behandling.opplysningTreFra(ALDERSKRAV_OPPLYSNING_NAVN)

private fun BehandlingDTO?.opplysningTreFra(opplysningstype: String) =
    this?.opplysning?.findLast { it.opplysningstype == opplysningstype }
