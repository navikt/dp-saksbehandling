package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Applikasjon
import java.util.UUID

class BehandlingOpprettetForSøknadHendelse(
    val ident: String,
    val søknadId: UUID,
    val behandlingId: UUID,
    override val utførtAv: Applikasjon = Applikasjon("dp-behandling"),
) : Hendelse(utførtAv)
