package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Applikasjon
import no.nav.dagpenger.saksbehandling.UtløstAvType
import java.util.UUID

data class BehandlingAvbruttHendelse(
    val behandlingId: UUID,
    val behandletHendelseId: String,
    val behandletHendelseType: UtløstAvType,
    val ident: String,
    override val utførtAv: Applikasjon = Applikasjon.DpBehandling,
) : Hendelse(utførtAv)
