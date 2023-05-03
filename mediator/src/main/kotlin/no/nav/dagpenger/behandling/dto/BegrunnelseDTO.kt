package no.nav.dagpenger.behandling.dto

import no.nav.dagpenger.behandling.oppgave.Saksbehandler
import java.time.LocalDateTime

interface BegrunnelseDTO {
    val kilde: String // quiz, saksbehandler, dingseboms
    val utført: LocalDateTime
}

internal data class SaksbehandlersBegrunnelseDTO(
    override val kilde: String = "Saksbehandler",
    override val utført: LocalDateTime,
    val utførtAv: Saksbehandler,
    val tekst: String,
) : BegrunnelseDTO

internal data class QuizBegrunnelseDTO(
    override val kilde: String = "Quiz",
    override val utført: LocalDateTime,
    val json: String,
) : BegrunnelseDTO
