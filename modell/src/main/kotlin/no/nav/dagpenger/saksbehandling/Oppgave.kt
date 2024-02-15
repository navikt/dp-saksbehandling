package no.nav.dagpenger.saksbehandling

import java.time.LocalDateTime
import java.util.UUID

data class Oppgave private constructor(
    val uuid: UUID,
    private val behandling: Behandling,
    val opprettet: LocalDateTime,
    private val _emneknagger: MutableSet<String>,
) {
    constructor(uuid: UUID, behandling: Behandling, emneknagger: Set<String> = emptySet()) : this(
        uuid = uuid,
        behandling = behandling,
        opprettet = LocalDateTime.now(),
        _emneknagger = emneknagger.toMutableSet(),
    )

    val emneknagger: Set<String>
        get() = _emneknagger.toSet()
}
