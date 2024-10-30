package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Applikasjon
import java.util.UUID

data class BehandlingOpplåstHendelse(
    val behandlingId: UUID,
    val ident: String,
    override val utførtAv: Applikasjon = Applikasjon("dp-behandling"),
) : Hendelse(utførtAv)
