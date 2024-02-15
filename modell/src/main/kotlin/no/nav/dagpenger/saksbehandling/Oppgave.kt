package no.nav.dagpenger.saksbehandling

import java.time.LocalDateTime
import java.util.UUID

data class Oppgave private constructor(
    val oppgaveId: UUID,
    val opprettet: LocalDateTime,
    private val _emneknagger: MutableSet<String>,
) {
    constructor(oppgaveId: UUID, emneknagger: Set<String> = emptySet()) : this(
        oppgaveId = oppgaveId,
        opprettet = LocalDateTime.now(),
        _emneknagger = emneknagger.toMutableSet(),
    )

    val emneknagger: Set<String>
        get() = _emneknagger.toSet()
}
