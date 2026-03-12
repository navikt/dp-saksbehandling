package no.nav.dagpenger.saksbehandling.tilbakekreving

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class TilbakekrevingHendelse(
    val ident: String,
    val eksternFagsakId: String,
    val eksternBehandlingId: String?,
    val tilbakekrevingBehandlingId: UUID,
    val totaltFeilutbetaltBeløp: BigDecimal,
    val opprettet: LocalDateTime,
    val status: BehandlingStatus,
) {
    enum class BehandlingStatus {
        OPPRETTET,
        TIL_BEHANDLING,
        TIL_GODKJENNING,
        AVSLUTTET,
    }
}
