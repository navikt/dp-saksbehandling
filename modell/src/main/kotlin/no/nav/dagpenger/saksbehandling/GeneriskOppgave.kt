package no.nav.dagpenger.saksbehandling

import java.time.LocalDateTime
import java.util.UUID

sealed class GeneriskOppgave(
    val oppgaveId: UUID,
    val opprettet: LocalDateTime,
    private val _emneknagger: MutableSet<String>,
    var behandlerIdent: String? = null,
    private val _tilstandslogg: Tilstandslogg = Tilstandslogg(),
)
