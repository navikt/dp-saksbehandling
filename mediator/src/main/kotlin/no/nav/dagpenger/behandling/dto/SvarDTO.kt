package no.nav.dagpenger.behandling.dto

import no.nav.dagpenger.behandling.Svar
import java.time.LocalDate

internal data class SvarDTO(
    val svar: String,
    val type: SvartypeDTO,
    val begrunnelse: BegrunnelseDTO,
) {

    fun toSvar(): Svar<*> {
        return when (this.type) {
            SvartypeDTO.String -> Svar(verdi = svar, String::class.java)
            SvartypeDTO.LocalDate -> Svar(verdi = LocalDate.parse(svar), LocalDate::class.java)
            SvartypeDTO.Int -> Svar<Int>(verdi = svar.toInt(), Int::class.java)
            SvartypeDTO.Boolean -> Svar<Boolean>(verdi = svar.toBoolean(), Boolean::class.java)
        }
    }
}

internal fun Svar<*>.toSvarDTO(): SvarDTO {
    val type = when (clazz.simpleName) {
        "Integer" -> SvartypeDTO.Int
        else -> SvartypeDTO.valueOf(clazz.simpleName.replaceFirstChar { it.uppercase() })
    }
    return SvarDTO(
        svar = this.verdi.toString(),
        type = type,
        begrunnelse = BegrunnelseDTO(kilde = "", tekst = ""),
    )
}

internal enum class SvartypeDTO {
    String,
    LocalDate,
    Int,
    Boolean,
}
