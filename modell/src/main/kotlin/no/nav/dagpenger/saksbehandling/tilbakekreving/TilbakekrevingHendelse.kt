package no.nav.dagpenger.saksbehandling.tilbakekreving

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class TilbakekrevingHendelse(
    val ident: String,
    val versjon: Int,
    val eksternFagsakId: String,
    val eksternBehandlingId: String?,
    val hendelseOpprettet: LocalDateTime,
    val tilbakekrevingBehandlingId: UUID,
    val sakOpprettet: LocalDateTime,
    val varselSendt: LocalDate?,
    val status: BehandlingStatus,
    val forrigeStatus: BehandlingStatus?,
    val totaltFeilutbetaltBeløp: BigDecimal,
    val saksbehandlingURL: String,
    val fullstendigPeriode: Periode,
) {
    data class Periode(
        val fom: LocalDate,
        val tom: LocalDate,
    )

    enum class BehandlingStatus {
        OPPRETTET,
        TIL_BEHANDLING,
        TIL_GODKJENNING,
        AVSLUTTET,
    }
}
