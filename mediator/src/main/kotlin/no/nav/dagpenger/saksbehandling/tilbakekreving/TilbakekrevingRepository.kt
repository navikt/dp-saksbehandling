package no.nav.dagpenger.saksbehandling.tilbakekreving

import no.nav.dagpenger.saksbehandling.db.Transaksjonskontekst
import no.nav.dagpenger.saksbehandling.hendelser.TilbakekrevingHendelse
import java.util.UUID

interface TilbakekrevingRepository {
    fun lagre(
        tilbakekreving: TilbakekrevingHendelse.Tilbakekreving,
        ctx: Transaksjonskontekst,
    )

    fun hent(behandlingId: UUID): TilbakekrevingHendelse.Tilbakekreving
}
