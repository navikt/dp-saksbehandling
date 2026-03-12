package no.nav.dagpenger.saksbehandling.tilbakekreving

import java.time.LocalDateTime
import java.util.UUID

data class TilbakekrevingHendelse(
    val ident: String,
    val eksternFagsakId: String,
    val eksternBehandlingId: String?,
    val hendelseOpprettet: LocalDateTime,
    val tilbakekrevingBehandlingId: UUID,
    val saksbehandlingURL: String,
)
