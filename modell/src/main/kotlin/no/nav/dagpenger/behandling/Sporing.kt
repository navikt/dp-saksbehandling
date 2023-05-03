package no.nav.dagpenger.behandling

import no.nav.dagpenger.behandling.oppgave.Saksbehandler
import java.time.LocalDateTime

sealed class Sporing(val utført: LocalDateTime)

class NullSporing() : Sporing(LocalDateTime.now())

class ManuellSporing(
    utført: LocalDateTime,
    val utførtAv: Saksbehandler,
    val begrunnelse: String,
) : Sporing(utført)

class QuizSporing(
    utført: LocalDateTime,
    val json: String,
) : Sporing(utført)
