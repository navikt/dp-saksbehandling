package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Applikasjon
import no.nav.dagpenger.saksbehandling.Behandler
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class TilbakekrevingOpprettetHendelse(
    val ident: String,
    val eksternFagsakId: String,
    val eksternBehandlingId: String?,
    val tilbakekrevingBehandlingId: UUID,
    val totaltFeilutbetaltBeløp: BigDecimal,
    val opprettet: LocalDateTime,
    override val utførtAv: Behandler = Applikasjon("familie-tilbake"),
) : Hendelse(utførtAv)
