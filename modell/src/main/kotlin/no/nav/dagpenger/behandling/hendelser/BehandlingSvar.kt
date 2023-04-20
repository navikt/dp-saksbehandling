package no.nav.dagpenger.behandling.hendelser

import java.util.UUID

class BehandlingSvar<T>(
    ident: String,
    val behandlingUUID: UUID,
    val stegUUID: UUID,
    val verdi: T,
) : Hendelse(ident)

class StegUtført(
    ident: String,
    val oppgaveUUID: UUID,
    val stegUUID: UUID,
)
