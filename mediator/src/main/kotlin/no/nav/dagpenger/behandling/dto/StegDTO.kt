package no.nav.dagpenger.behandling.dto

import no.nav.dagpenger.behandling.Steg
import no.nav.dagpenger.behandling.Tilstand
import no.nav.dagpenger.behandling.api.models.StegDTO
import no.nav.dagpenger.behandling.api.models.StegtypeDTO
import no.nav.dagpenger.behandling.api.models.TilstandDTO

internal fun Collection<Steg<*>>.toStegDTO(): List<StegDTO> = this.map { it.toStegDTO() }

internal fun Steg<*>.toStegDTO(): StegDTO {
    val stegtypeDTO = when (this) {
        is Steg.Fastsettelse<*> -> StegtypeDTO.Fastsetting
        is Steg.Vilkår -> StegtypeDTO.Vilkår
        is Steg.Prosess -> StegtypeDTO.Prosess
    }
    val tilstand = this.tilstand
    val svarDTO = this.svar.toSvarDTO()
    return StegDTO(
        uuid = this.uuid,
        id = this.id,
        type = stegtypeDTO,
        svartype = svarDTO.type,
        tilstand = tilstand.toTilstandDTO(),
        svar = svarDTO,
    )
}

internal fun Tilstand.toTilstandDTO(): TilstandDTO {
    return TilstandDTO.valueOf(this.name)
}
