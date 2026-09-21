package no.nav.dagpenger.saksbehandling.tilbakekreving

import no.nav.dagpenger.saksbehandling.db.Transaksjonskontekst
import no.nav.dagpenger.saksbehandling.hendelser.Tilbakekreving
import java.util.UUID

interface TilbakekrevingRepository {
    fun lagre(
        tilbakekreving: Tilbakekreving,
        ctx: Transaksjonskontekst,
    )

    fun hent(behandlingId: UUID): Tilbakekreving
}
