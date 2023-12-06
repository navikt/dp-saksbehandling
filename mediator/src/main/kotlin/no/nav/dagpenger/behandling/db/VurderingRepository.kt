package no.nav.dagpenger.behandling.db

import no.nav.dagpenger.behandling.MinsteinntektVurdering
import java.util.UUID

interface VurderingRepository {
    fun hentMinsteInntektVurdering(oppgaveId: UUID): MinsteinntektVurdering
}


