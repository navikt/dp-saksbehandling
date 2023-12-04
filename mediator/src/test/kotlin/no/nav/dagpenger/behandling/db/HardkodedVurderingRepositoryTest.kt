package no.nav.dagpenger.behandling.db

import org.junit.jupiter.api.Test

class HardkodedVurderingRepositoryTest {
    @Test
    fun `Skal kunne hente ut fra repository`() {
        HardkodedVurderingRepository().hentMinsteInntektVurdering(oppgaveId = java.util.UUID.randomUUID())
            .let { vurdering ->
                println(vurdering)
            }
    }
}
