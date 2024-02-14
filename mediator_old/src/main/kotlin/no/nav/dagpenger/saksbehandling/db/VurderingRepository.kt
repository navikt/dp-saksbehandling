package no.nav.dagpenger.saksbehandling.db

import no.nav.dagpenger.saksbehandling.MinsteInntektVurdering
import java.util.UUID

interface VurderingRepository {
    fun hentMinsteInntektVurdering(oppgaveId: UUID): MinsteInntektVurdering

    fun lagreMinsteInntektVurdering(
        oppgaveId: UUID,
        vurdering: MinsteInntektVurdering,
    )
}
