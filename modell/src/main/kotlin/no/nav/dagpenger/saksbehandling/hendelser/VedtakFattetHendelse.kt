package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Applikasjon
import no.nav.dagpenger.saksbehandling.Behandler
import no.nav.dagpenger.saksbehandling.UtsendingSak
import java.util.UUID

data class VedtakFattetHendelse(
    val behandlingId: UUID,
    val behandletHendelseId: String,
    val behandletHendelseType: String,
    val ident: String,
    val sak: UtsendingSak?,
    val automatiskBehandlet: Boolean? = null,
    val saksbehandlerIdent: String? = null,
    val beslutterIdent: String? = null,
    override val utførtAv: Behandler = Applikasjon.DpBehandling,
) : Hendelse(utførtAv)
