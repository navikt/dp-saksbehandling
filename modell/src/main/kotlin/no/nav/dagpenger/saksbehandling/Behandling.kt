package no.nav.dagpenger.saksbehandling

import no.nav.dagpenger.saksbehandling.hendelser.PersonHendelse
import java.time.LocalDateTime
import java.util.UUID

class Behandling private constructor(
    val person: Person,
    val opprettet: LocalDateTime,
    val uuid: UUID,
    val behandler: List<PersonHendelse>,
) {
    constructor(person: Person, hendelse: PersonHendelse) : this(
        person,
        LocalDateTime.now(),
        UUID.randomUUID(),
        listOf(hendelse),
    )
}
