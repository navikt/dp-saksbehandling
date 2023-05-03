package no.nav.dagpenger.behandling.dto

import no.nav.dagpenger.behandling.ManuellSporing
import no.nav.dagpenger.behandling.NullSporing
import no.nav.dagpenger.behandling.QuizSporing
import no.nav.dagpenger.behandling.Sporing
import no.nav.dagpenger.behandling.Svar
import no.nav.dagpenger.behandling.oppgave.Saksbehandler
import java.time.LocalDateTime

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

fun Sporing.toBegrunnelseDTO(): BegrunnelseDTO? {
    return when (this) {
        is QuizSporing -> QuizBegrunnelseDTO(utført = LocalDateTime.now(), json = "{}")
        is ManuellSporing -> SaksbehandlersBegrunnelseDTO(
            utført = LocalDateTime.now(),
            utførtAv = Saksbehandler(),
            tekst = "Jeg sjekka a-inntekt",
        )
        is NullSporing -> null
    }
}

internal enum class SvartypeDTO {
    String,
    LocalDate,
    Int,
    Boolean,
    Double,
}
