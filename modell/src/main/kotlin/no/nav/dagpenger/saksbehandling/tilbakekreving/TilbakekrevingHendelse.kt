package no.nav.dagpenger.saksbehandling.tilbakekreving

import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

sealed class TilbakekrevingHendelse {
    abstract val ident: String
    abstract val eksternFagsakId: String
    abstract val eksternBehandlingId: String?
    abstract val tilbakekrevingBehandlingId: UUID
    abstract val totaltFeilutbetaltBeløp: BigDecimal
    abstract val opprettet: LocalDateTime

    data class Opprettet(
        override val ident: String,
        override val eksternFagsakId: String,
        override val eksternBehandlingId: String?,
        override val tilbakekrevingBehandlingId: UUID,
        override val totaltFeilutbetaltBeløp: BigDecimal,
        override val opprettet: LocalDateTime,
    ) : TilbakekrevingHendelse()

    data class TilBehandling(
        override val ident: String,
        override val eksternFagsakId: String,
        override val eksternBehandlingId: String?,
        override val tilbakekrevingBehandlingId: UUID,
        override val totaltFeilutbetaltBeløp: BigDecimal,
        override val opprettet: LocalDateTime,
    ) : TilbakekrevingHendelse()

    data class TilGodkjenning(
        override val ident: String,
        override val eksternFagsakId: String,
        override val eksternBehandlingId: String?,
        override val tilbakekrevingBehandlingId: UUID,
        override val totaltFeilutbetaltBeløp: BigDecimal,
        override val opprettet: LocalDateTime,
    ) : TilbakekrevingHendelse()

    data class Avsluttet(
        override val ident: String,
        override val eksternFagsakId: String,
        override val eksternBehandlingId: String?,
        override val tilbakekrevingBehandlingId: UUID,
        override val totaltFeilutbetaltBeløp: BigDecimal,
        override val opprettet: LocalDateTime,
    ) : TilbakekrevingHendelse()
}
