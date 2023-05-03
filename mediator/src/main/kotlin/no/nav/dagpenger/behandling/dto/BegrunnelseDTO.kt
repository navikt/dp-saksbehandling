package no.nav.dagpenger.behandling.dto

import no.nav.dagpenger.behandling.ManuellSporing
import no.nav.dagpenger.behandling.NullSporing
import no.nav.dagpenger.behandling.QuizSporing
import no.nav.dagpenger.behandling.Sporing
import no.nav.dagpenger.behandling.oppgave.Saksbehandler
import java.time.LocalDateTime

interface BegrunnelseDTO {
    val kilde: Kilde
    val utført: LocalDateTime
}

internal data class SaksbehandlersBegrunnelseDTO(
    override val kilde: Kilde = Kilde.Saksbehandler,
    override val utført: LocalDateTime,
    val utførtAv: Saksbehandler,
    val tekst: String,
) : BegrunnelseDTO

internal data class QuizBegrunnelseDTO(
    override val kilde: Kilde = Kilde.Quiz,
    override val utført: LocalDateTime,
    val json: String,
) : BegrunnelseDTO

fun Sporing.toBegrunnelseDTO(): BegrunnelseDTO? {
    return when (this) {
        is QuizSporing -> QuizBegrunnelseDTO(
            utført = this.utført,
            json = this.json,
        )

        is ManuellSporing -> SaksbehandlersBegrunnelseDTO(
            utført = this.utført,
            utførtAv = this.utførtAv,
            tekst = this.begrunnelse,
        )

        is NullSporing -> null
    }
}

enum class Kilde {
    Quiz,
    Saksbehandler,
}
