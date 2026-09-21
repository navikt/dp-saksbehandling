package no.nav.dagpenger.saksbehandling.tilbakekreving

import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class Tilbakekreving(
    val behandlingId: UUID,
    val opprettet: LocalDateTime,
    val avventBehandlingTilDato: LocalDate?,
    val varselSendt: LocalDate?,
    val behandlingsstatus: BehandlingStatus,
    val forrigeBehandlingsstatus: BehandlingStatus?,
    val totaltFeilutbetaltBeløp: BigDecimal,
    val saksbehandlingURL: String,
    val fullstendigPeriode: Periode,
) {
    data class Periode(
        val fom: LocalDate,
        val tom: LocalDate,
    )

    enum class BehandlingStatus {
        TIL_FORHÅNDSVARSEL,
        TIL_BEHANDLING,
        TIL_GODKJENNING,
        AVSLUTTET,
    }
}
