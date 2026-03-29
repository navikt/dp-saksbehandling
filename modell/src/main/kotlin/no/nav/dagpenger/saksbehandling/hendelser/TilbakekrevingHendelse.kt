package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Applikasjon
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class TilbakekrevingHendelse(
    val ident: String,
    val eksternFagsakId: String,
    val eksternBehandlingId: UUID,
    val hendelseOpprettet: LocalDateTime,
    val tilbakekreving: Tilbakekreving,
    override val utførtAv: Applikasjon = Applikasjon.Tilbakekreving,
) : Hendelse(utførtAv) {
    data class Tilbakekreving(
        val behandlingId: UUID,
        val sakOpprettet: LocalDateTime,
        val varselSendt: LocalDate?,
        val behandlingsstatus: BehandlingStatus,
        val forrigeBehandlingsstatus: BehandlingStatus?,
        val totaltFeilutbetaltBeløp: BigDecimal,
        val saksbehandlingURL: String,
        val fullstendigPeriode: Periode,
    )

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
