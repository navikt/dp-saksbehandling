package no.nav.dagpenger.saksbehandling.api

import de.slub.urn.URN
import no.nav.dagpenger.behandling.opplysninger.api.models.BehandlingDTO
import no.nav.dagpenger.saksbehandling.Steg

const val ALDERSKRAV_OPPLYSNING_NAVN = "Oppfyller kravet til alder"
val alderStegUrn = URN.rfc8141().parse("urn:steg:alder")

fun alderskravStegFra(behandlingDTO: BehandlingDTO?): Steg? {
    val alderskravOpplysningsTre = alderskravOpplysningFra(behandlingDTO)
    return when {
        alderskravOpplysningsTre != null ->
            Steg(
                urn = alderStegUrn,
                opplysninger = hentAlleUnikeOpplysningerFra(alderskravOpplysningsTre),
            )

        else -> null
    }
}

private fun alderskravOpplysningFra(behandling: BehandlingDTO?) =
    behandling?.opplysning?.findLast { it.opplysningstype == ALDERSKRAV_OPPLYSNING_NAVN }
