package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Applikasjon
import no.nav.dagpenger.saksbehandling.UtløstAvType
import java.time.LocalDateTime
import java.util.UUID

data class GenerellBehandlingOpprettetHendelse(
    val behandlingId: UUID,
    val ident: String,
    val opprettet: LocalDateTime,
    val type: UtløstAvType,
    val behandletHendelseId: String,
    val basertPåBehandling: UUID? = null,
    val behandlingskjedeId: UUID? = null,
    override val utførtAv: Applikasjon = Applikasjon.DpBehandling,
) : Hendelse(utførtAv)
