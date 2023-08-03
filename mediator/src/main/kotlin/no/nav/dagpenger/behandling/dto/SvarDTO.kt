package no.nav.dagpenger.behandling.dto

import no.nav.dagpenger.behandling.Svar
import no.nav.dagpenger.behandling.api.models.SvarDTO
import no.nav.dagpenger.behandling.api.models.SvartypeDTO

internal fun Svar<*>.toSvarDTO(): SvarDTO {
    val type = when (clazz.simpleName) {
        "Integer" -> SvartypeDTO.Int
        "String" -> SvartypeDTO.Strings // TODO: Hack for å komme rundt bug i openapi-generator - https://github.com/androa/openapi-generator/pull/1
        else -> SvartypeDTO.valueOf(clazz.simpleName.replaceFirstChar { it.uppercase() })
    }
    return SvarDTO(
        svar = this.verdi?.toString(),
        type = type,
        begrunnelse = this.sporing.toBegrunnelseDTO(),
    )
}
