package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Applikasjon
import no.nav.dagpenger.saksbehandling.Sak
import java.util.UUID

data class VedtakFattetHendelse(
    val behandlingId: UUID,
    val søknadId: UUID,
    val ident: String,
    val sak: Sak,
    val automatiskBehandlet: Boolean? = null,
    override val utførtAv: Applikasjon = Applikasjon("dp-behandling"),
) : Hendelse(utførtAv)
