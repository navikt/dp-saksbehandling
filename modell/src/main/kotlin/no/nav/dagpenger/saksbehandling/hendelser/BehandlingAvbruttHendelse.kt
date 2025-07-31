package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Applikasjon
import java.util.UUID

data class BehandlingAvbruttHendelse(
    val behandlingId: UUID,
    val behandletHendelseId: String,
    val behandletHendelseType: String,
    val ident: String,
    override val utførtAv: Applikasjon = Applikasjon("dp-behandling"),
) : Hendelse(utførtAv)
