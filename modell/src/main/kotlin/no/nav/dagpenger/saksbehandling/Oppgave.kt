package no.nav.dagpenger.saksbehandling

import java.time.LocalDateTime
import java.util.UUID

data class Oppgave private constructor(
    val oppgaveId: UUID,
    val opprettet: LocalDateTime,
    val ident: String,
    private val _emneknagger: MutableSet<String>,
    val steg: MutableList<Steg>,
    val behandlingId: UUID,
) {
    constructor(
        oppgaveId: UUID,
        ident: String,
        emneknagger: Set<String> = emptySet(),
        opprettet: LocalDateTime,
        behandlingId: UUID,
    ) : this(
        oppgaveId = oppgaveId,
        ident = ident,
        opprettet = opprettet,
        _emneknagger = emneknagger.toMutableSet(),
        steg = mutableListOf(),
        behandlingId = behandlingId,
    )

    val emneknagger: Set<String>
        get() = _emneknagger.toSet()
}
