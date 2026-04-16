package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Applikasjon
import no.nav.dagpenger.saksbehandling.UtløstAvType
import no.nav.dagpenger.saksbehandling.UtsendingSak
import java.util.UUID

data class VedtakFattetHendelse(
    val behandlingId: UUID,
    val behandletHendelseId: String,
    val behandletHendelseType: UtløstAvType,
    val ident: String,
    val sak: UtsendingSak?,
    val automatiskBehandlet: Boolean? = null,
    override val utførtAv: Applikasjon = Applikasjon.DpBehandling,
) : Hendelse(utførtAv)
