package no.nav.dagpenger.behandling.hendelser

import java.util.UUID

class StegUtført(
    ident: String,
    val oppgaveUUID: UUID,
) : Hendelse(UUID.randomUUID(), ident)
