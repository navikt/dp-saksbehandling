package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Applikasjon
import no.nav.dagpenger.saksbehandling.UtløstAvType
import java.util.UUID

data class ForslagTilVedtakHendelse(
    val ident: String,
    val behandletHendelseId: String,
    val behandletHendelseType: UtløstAvType,
    val behandlingId: UUID,
    val emneknagger: Set<String> = emptySet(),
    override val utførtAv: Applikasjon = Applikasjon.DpBehandling,
) : Hendelse(utførtAv)
