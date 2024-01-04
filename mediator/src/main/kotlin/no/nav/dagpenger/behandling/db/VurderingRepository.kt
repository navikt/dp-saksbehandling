package no.nav.dagpenger.behandling.db

import no.nav.dagpenger.behandling.MinsteInntektVurdering
import java.util.UUID

interface VurderingRepository {
    fun hentMinsteInntektVurdering(oppgaveId: UUID): MinsteInntektVurdering

    fun lagreMinsteInntektVurdering(
        oppgaveId: UUID,
        vurdering: MinsteInntektVurdering,
    )
}
