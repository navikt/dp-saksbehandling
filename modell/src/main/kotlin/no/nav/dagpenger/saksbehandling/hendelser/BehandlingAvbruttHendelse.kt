package no.nav.dagpenger.saksbehandling.hendelser

import java.util.UUID

data class BehandlingAvbruttHendelse(
    val behandlingId: UUID,
    val søknadId: UUID,
    val ident: String,
    override val utførtAv: String = "dp-behandling",
) : Hendelse(utførtAv)
