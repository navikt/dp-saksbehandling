package no.nav.dagpenger.behandling.dto

import no.nav.dagpenger.behandling.Steg
import no.nav.dagpenger.behandling.Tilstand
import java.util.UUID

internal data class StegDTO(
    val uuid: UUID,
    val id: String, // reell arbeidssøker, vurder minsteinntekt, fastsett virkningstidspunkt, fastsett vanlig arbeidstid
    val type: StegtypeDTO,
    val svartype: SvartypeDTO,
    val tilstand: Tilstand,
    val svar: SvarDTO? = null,
)

internal fun Collection<Steg<*>>.toStegDTO(): List<StegDTO> = this.map { it.toStegDTO() }

internal fun Steg<*>.toStegDTO(): StegDTO {
    val stegtypeDTO = when (this) {
        is Steg.Fastsettelse<*> -> StegtypeDTO.Fastsetting
        is Steg.Vilkår -> StegtypeDTO.Vilkår
    }
    val tilstand = this.tilstand
    val svarDTO = this.svar.toSvarDTO()
    return StegDTO(
        uuid = this.uuid,
        id = this.id,
        type = stegtypeDTO,
        svartype = svarDTO.type,
        tilstand = tilstand,
        svar = svarDTO,
    )
}

internal enum class StegtypeDTO {
    Fastsetting,
    Vilkår,
}
