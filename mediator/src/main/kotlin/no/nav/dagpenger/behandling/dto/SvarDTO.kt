package no.nav.dagpenger.behandling.dto

import no.nav.dagpenger.behandling.Svar

internal data class SvarDTO(
    val svar: String?,
    val type: SvartypeDTO,
    val begrunnelse: BegrunnelseDTO?,
)

internal fun Svar<*>.toSvarDTO(): SvarDTO {
    val type = when (clazz.simpleName) {
        "Integer" -> SvartypeDTO.Int
        else -> SvartypeDTO.valueOf(clazz.simpleName.replaceFirstChar { it.uppercase() })
    }
    return SvarDTO(
        svar = this.verdi?.toString(),
        type = type,
        begrunnelse = this.sporing.toBegrunnelseDTO(),
    )
}

internal enum class SvartypeDTO {
    String,
    LocalDate,
    Int,
    Boolean,
    Double,
}
