package no.nav.dagpenger.saksbehandling

import java.time.ZonedDateTime
import java.util.UUID

data class Oppgave private constructor(
    val oppgaveId: UUID,
    val opprettet: ZonedDateTime,
    val ident: String,
    private val _emneknagger: MutableSet<String>,
    val behandlingId: UUID,
    val steg: MutableList<Steg>,
) {
    constructor(
        oppgaveId: UUID,
        ident: String,
        emneknagger: Set<String> = emptySet(),
        opprettet: ZonedDateTime,
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
