package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Applikasjon
import java.util.UUID

data class ForslagTilVedtakHendelse(
    val ident: String,
    val søknadId: UUID,
    val behandlingId: UUID,
    val emneknagger: Set<String> = emptySet(),
    override val utførtAv: Applikasjon = Applikasjon("dp-behandling"),
) : Hendelse(utførtAv)
