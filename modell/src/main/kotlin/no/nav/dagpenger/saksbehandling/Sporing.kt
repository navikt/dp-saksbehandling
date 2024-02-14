package no.nav.dagpenger.saksbehandling

import java.time.LocalDateTime

sealed class Sporing(val utført: LocalDateTime)

object NullSporing : Sporing(LocalDateTime.MIN)

class ManuellSporing(
    utført: LocalDateTime,
    val utførtAv: Saksbehandler,
    val begrunnelse: String,
) : Sporing(utført)

class QuizSporing(
    utført: LocalDateTime,
    val json: String,
) : Sporing(utført)
