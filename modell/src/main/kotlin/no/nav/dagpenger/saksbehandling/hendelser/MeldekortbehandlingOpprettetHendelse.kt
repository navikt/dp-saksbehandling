package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Applikasjon
import java.time.LocalDateTime
import java.util.UUID

data class MeldekortbehandlingOpprettetHendelse(
    val meldekortId: String,
    val behandlingId: UUID,
    val ident: String,
    val opprettet: LocalDateTime,
    val basertPåBehandling: UUID,
    override val utførtAv: Applikasjon = Applikasjon("dp-behandling"),
) : Hendelse(utførtAv)
