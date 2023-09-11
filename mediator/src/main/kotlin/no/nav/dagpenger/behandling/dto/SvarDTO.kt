package no.nav.dagpenger.behandling.dto

import no.nav.dagpenger.behandling.Svar
import no.nav.dagpenger.behandling.api.models.SvarDTO
import no.nav.dagpenger.behandling.api.models.SvartypeDTO

internal fun Svar<*>.toSvarDTO(): SvarDTO {
    val type = when (this) {
        is Svar.BooleanSvar -> SvartypeDTO.Boolean
        is Svar.DoubleSvar -> SvartypeDTO.Double
        is Svar.IntegerSvar -> SvartypeDTO.Int
        is Svar.LocalDateSvar -> SvartypeDTO.LocalDate
        is Svar.StringSvar -> SvartypeDTO.String
    }
    return SvarDTO(
        svar = this.verdi?.toString(),
        type = type,
        begrunnelse = this.sporing.toBegrunnelseDTO(),
    )
}
