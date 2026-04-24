package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Applikasjon
import no.nav.dagpenger.saksbehandling.HendelseBehandler
import java.time.LocalDateTime
import java.util.UUID

data class DpBehandlingOpprettetHendelse(
    val behandlingId: UUID,
    val ident: String,
    val opprettet: LocalDateTime,
    val basertPåBehandling: UUID?,
    val behandlingskjedeId: UUID,
    val type: HendelseBehandler,
    val eksternId: String? = null,
    override val utførtAv: Applikasjon = Applikasjon.DpBehandling,
) : Hendelse(utførtAv)
