package no.nav.dagpenger.saksbehandling.hendelser

import no.nav.dagpenger.saksbehandling.Applikasjon
import java.time.LocalDateTime
import java.util.UUID

data class TilbakekrevingHendelse(
    val eksternBehandlingId: UUID,
    val hendelseOpprettet: LocalDateTime,
    val tilbakekreving: Tilbakekreving,
    override val utførtAv: Applikasjon = Applikasjon.Tilbakekreving,
) : Hendelse(utførtAv) {
    override fun toString(): String =
        "TilbakekrevingHendelse(eksternBehandlingId=$eksternBehandlingId, " +
            "hendelseOpprettet=$hendelseOpprettet, tilbakekreving=$tilbakekreving, utførtAv=$utførtAv)"
}
